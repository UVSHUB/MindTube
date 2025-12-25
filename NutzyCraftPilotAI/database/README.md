# Database and Caching Setup

## 1. Supabase (PostgreSQL) setup

1.  Log in to your [Supabase Dashboard](https://supabase.com/dashboard).
2.  Create a new project.
3.  Go to the **SQL Editor** in the side navigation.
4.  Click **New query**.
5.  Copy the contents of `schema.sql` from this directory.
6.  Paste it into the SQL Editor and click **Run**.

This will create the necessary tables (`users`, `analysis_reports`, `daily_trends`) and apply Row Level Security (RLS) policies.

## 2. Redis Setup

We use Redis for caching trend data and managing the analysis job queue.

### Running Locally with Docker

Ensure you have Docker installed and running.

1.  Navigate to the project root directory (where `docker-compose.yml` is located).
2.  Run the following command:

    ```bash
    docker-compose up -d
    ```

3.  Redis will be available at `localhost:6379`.

### Queue Structure

*   **Job Queue**: `queue:analysis_jobs`
*   **Job Status**: `job:{jobId}:status`
