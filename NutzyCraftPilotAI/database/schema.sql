-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- 1. Users Table (Profile Table)
-- Note: This schema supports Spring Boot authentication
-- If using Supabase Auth instead, change to:
--   id uuid references auth.users on delete cascade primary key,
--   and remove email, password, verification_code, is_verified columns
create table public.users (
                              id uuid default uuid_generate_v4() primary key,
                              full_name text,
                              avatar_url text,
                              created_at timestamp with time zone default timezone('utc'::text, now()) not null,
                              -- Additional fields for Spring Boot authentication
                              email varchar(255) unique,
                              password varchar(255),
                              verification_code varchar(255),
                              is_verified boolean default false
);

-- 2. Analysis Reports
create type analysis_source_type as enum ('url', 'file');
create type analysis_status as enum ('pending', 'processing', 'completed', 'failed');

create table public.analysis_reports (
                                         id uuid default uuid_generate_v4() primary key,
                                         user_id uuid references public.users(id) on delete cascade not null,
                                         source_type analysis_source_type not null,
                                         source_url text,
                                         file_names text[],
                                         title text default 'Untitled Analysis',
                                         summary text,
                                         craft_score integer,
                                         metrics jsonb default '{}'::jsonb,
                                         status analysis_status default 'pending',
                                         created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 3. Daily Trends
create table public.daily_trends (
                                     id uuid default uuid_generate_v4() primary key,
                                     user_id uuid references public.users(id) on delete cascade not null,
                                     date date default current_date not null,
                                     analysis_count integer default 0,
                                     avg_score float default 0,
                                     created_at timestamp with time zone default timezone('utc'::text, now()) not null,
                                     unique(user_id, date)
);

-- 4. User Settings
create table public.user_settings (
                                      user_id uuid references public.users(id) on delete cascade primary key,
                                      email_notifications boolean default true,
                                      marketing_emails boolean default false,
                                      updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- 5. Contact Submissions
create table public.contact_submissions (
    id uuid default uuid_generate_v4() primary key,
    name text not null,
    email text not null,
    subject text not null,
    message text not null,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- RLS SETTINGS
alter table public.users enable row level security;
alter table public.analysis_reports enable row level security;
alter table public.daily_trends enable row level security;
alter table public.user_settings enable row level security;
alter table public.contact_submissions enable row level security;

-- Example of a more complete policy for Reports (Select, Insert, Delete)
create policy "Users can manage their own reports"
  on public.analysis_reports for all
  using ( auth.uid() = user_id );

-- Public contact submission policy
create policy "Anyone can submit contact form"
  on public.contact_submissions for insert
  with check ( true );

  -- 1. Create the function that handles the new user
  -- Note: This trigger is for Supabase Auth integration
  -- If using Spring Boot authentication only, you can skip this trigger
create or replace function public.handle_new_user()
returns trigger as $$
begin
  -- Insert into public.users
  insert into public.users (id, full_name, avatar_url)
  values (
    new.id, 
    new.raw_user_meta_data->>'full_name', 
    new.raw_user_meta_data->>'avatar_url'
  );

  -- Insert into public.user_settings
  insert into public.user_settings (user_id)
  values (new.id);

  return new;
end;
$$ language plpgsql security definer;

-- 2. Create the trigger to run the function after a signup
-- Note: Only works if using Supabase Auth (auth.users table exists)
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

  -- 1. Create the function to update daily statistics
create or replace function public.update_daily_trends()
returns trigger as $$
begin
  -- Only update if the status has changed to 'completed'
  if (new.status = 'completed' and (old.status is null or old.status != 'completed')) then
    
    insert into public.daily_trends (user_id, date, analysis_count, avg_score)
    values (
      new.user_id, 
      current_date, 
      1, 
      new.craft_score
    )
    on conflict (user_id, date) 
    do update set 
      -- Increment the count
      analysis_count = daily_trends.analysis_count + 1,
      -- Calculate the new average: ((old_avg * old_count) + new_score) / new_count
      avg_score = ((daily_trends.avg_score * daily_trends.analysis_count) + new.craft_score) / (daily_trends.analysis_count + 1);
      
  end if;
  return new;
end;
$$ language plpgsql security definer;

-- 2. Attach the trigger to the analysis_reports table
create trigger on_analysis_completed
  after update on public.analysis_reports
  for each row
  execute procedure public.update_daily_trends();