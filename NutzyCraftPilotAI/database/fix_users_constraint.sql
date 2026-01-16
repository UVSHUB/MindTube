-- Fix for users table foreign key constraint error
-- This script removes the incorrect foreign key constraint on users.id

-- First, check if the constraint exists and drop it
DO $$
BEGIN
    -- Drop the foreign key constraint if it exists
    IF EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'users_id_fkey'
    ) THEN
        ALTER TABLE public.users DROP CONSTRAINT users_id_fkey;
        RAISE NOTICE 'Dropped constraint users_id_fkey';
    ELSE
        RAISE NOTICE 'Constraint users_id_fkey does not exist';
    END IF;
END $$;

-- Verify the users table structure
-- The id column should be a PRIMARY KEY, not a FOREIGN KEY
-- If for some reason the primary key is missing, add it back
DO $$
BEGIN
    -- Check if primary key exists
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'users_pkey' 
        AND contype = 'p'
    ) THEN
        ALTER TABLE public.users ADD PRIMARY KEY (id);
        RAISE NOTICE 'Added primary key constraint to users.id';
    ELSE
        RAISE NOTICE 'Primary key constraint already exists on users.id';
    END IF;
END $$;

-- Display current constraints on users table
SELECT 
    conname AS constraint_name,
    contype AS constraint_type,
    pg_get_constraintdef(oid) AS constraint_definition
FROM pg_constraint
WHERE conrelid = 'public.users'::regclass
ORDER BY conname;
