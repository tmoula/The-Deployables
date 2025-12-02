-- Add verification fields to users table
ALTER TABLE users ADD COLUMN verification_code VARCHAR(10);
ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE;

-- Update existing users to be verified (optional, but good for dev)
UPDATE users SET is_verified = TRUE WHERE is_verified IS NULL;
