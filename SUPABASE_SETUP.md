# Supabase Database Backup Setup

## Overview
This new backup system uses Supabase database tables instead of Storage API, which is much more reliable and easier to work with.

## Step 1: Create the Backups Table

Go to your Supabase Dashboard → SQL Editor and run this SQL:

```sql
-- Create the backups table
CREATE TABLE IF NOT EXISTS backups (
    id TEXT PRIMARY KEY,
    data TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create an index for faster queries
CREATE INDEX IF NOT EXISTS idx_backups_created_at ON backups(created_at DESC);

-- Enable Row Level Security (RLS)
ALTER TABLE backups ENABLE ROW LEVEL SECURITY;

-- Create policy to allow anonymous access for backups
CREATE POLICY "Allow anonymous backup access" ON backups
    FOR ALL
    TO anon
    USING (true)
    WITH CHECK (true);

-- Grant necessary permissions
GRANT ALL ON backups TO anon;
GRANT USAGE ON SEQUENCE backups_id_seq TO anon;
```

## Step 2: Test the Setup

After creating the table, you can test it with these SQL commands:

```sql
-- Test insert
INSERT INTO backups (id, data) VALUES ('test_backup', '{"test": "data"}');

-- Test select
SELECT * FROM backups ORDER BY created_at DESC;

-- Clean up test data
DELETE FROM backups WHERE id = 'test_backup';
```

## Step 3: Update Your App

The app has been updated to use this new database-based system. The changes include:

1. **Backup Creation**: Stores backup data in the `backups` table
2. **Backup Listing**: Queries the `backups` table to list available backups
3. **Backup Restoration**: Downloads backup data from the `backups` table

## Benefits of This Approach

✅ **More Reliable**: Uses standard REST API instead of Storage API
✅ **Easier to Debug**: Standard database queries and responses
✅ **Better Performance**: Direct database access
✅ **No Storage Policies**: No need to configure complex storage policies
✅ **Standard REST**: Uses the well-documented Supabase REST API

## How It Works

1. **Create Backup**: App creates JSON backup data and stores it in the `backups` table
2. **List Backups**: App queries the `backups` table to get all available backups
3. **Restore Backup**: App downloads backup data from the `backups` table and restores it

## API Endpoints Used

- **POST** `/rest/v1/backups` - Create new backup
- **GET** `/rest/v1/backups?select=*&order=created_at.desc` - List all backups
- **GET** `/rest/v1/backups?id=eq.{backup_id}&select=data` - Get specific backup

This approach is much more reliable than the Storage API and should work consistently!
