-- Enable UUID extension
create extension if not exists "uuid-ossp";

-- 1. Users Table (extends Supabase auth.users)
  id uuid default uuid_generate_v4() primary key,
  email text unique not null,
  password text not null,
  full_name text,
  avatar_url text,
  verification_code text, -- For email verification
  is_verified boolean default false, -- Track verification status
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- RLS for Users
alter table public.users enable row level security;

create policy "Users can view their own profile"
  on public.users for select
  using ( auth.uid() = id );

create policy "Users can update their own profile"
  on public.users for update
  using ( auth.uid() = id );

-- 2. Analysis Reports Table
create type analysis_source_type as enum ('url', 'file');
create type analysis_status as enum ('pending', 'processing', 'completed', 'failed');

create table public.analysis_reports (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.users(id) not null,
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

-- RLS for Analysis Reports
alter table public.analysis_reports enable row level security;

create policy "Users can view their own reports"
  on public.analysis_reports for select
  using ( auth.uid() = user_id );

create policy "Users can create their own reports"
  on public.analysis_reports for insert
  with check ( auth.uid() = user_id );

-- 3. Daily Trends Table
create table public.daily_trends (
  id uuid default uuid_generate_v4() primary key,
  user_id uuid references public.users(id) not null,
  date date default current_date not null,
  analysis_count integer default 0,
  avg_score float default 0,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null,
  unique(user_id, date)
);

-- RLS for Daily Trends
alter table public.daily_trends enable row level security;

create policy "Users can view their own trends"
  on public.daily_trends for select
  using ( auth.uid() = user_id );

-- 4. User Settings Table
create table public.user_settings (
  user_id uuid references public.users(id) primary key,
  email_notifications boolean default true,
  marketing_emails boolean default false,
  updated_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- RLS for User Settings
alter table public.user_settings enable row level security;

create policy "Users can view their own settings"
  on public.user_settings for select
  using ( auth.uid() = user_id );

create policy "Users can update their own settings"
  on public.user_settings for update
  using ( auth.uid() = user_id );

create policy "Users can insert their own settings"
  on public.user_settings for insert
  with check ( auth.uid() = user_id );

-- 5. Contact Submissions Table
create table public.contact_submissions (
  id uuid default uuid_generate_v4() primary key,
  name text not null,
  email text not null,
  subject text not null,
  message text not null,
  created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- RLS for Contact Submissions
alter table public.contact_submissions enable row level security;

create policy "Anyone can insert contact message"
  on public.contact_submissions for insert
  with check ( true );
