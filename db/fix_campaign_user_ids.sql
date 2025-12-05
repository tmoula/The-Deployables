-- Diagnostic and Fix Script for Campaign User ID Issues
-- Run this in DBeaver to understand and fix the user_id problem

-- 1. Check all users and their IDs (THIS IS THE KEY QUERY!)
SELECT id, email, created_at 
FROM users 
ORDER BY id;

-- 1b. Check if multiple users have the same ID (THIS WOULD BE A PROBLEM!)
SELECT id, COUNT(*) as user_count, STRING_AGG(email, ', ') as emails
FROM users
GROUP BY id
HAVING COUNT(*) > 1;

-- 2. Check all campaigns and their user_ids
SELECT campaign_id, user_id, campaign_name, created_at 
FROM campaigns 
ORDER BY campaign_id;

-- 3. Check which campaigns belong to which users (proper JOIN)
SELECT 
    c.campaign_id,
    c.user_id,
    c.campaign_name,
    u.email,
    u.id as user_table_id,
    c.created_at
FROM campaigns c
LEFT JOIN users u ON c.user_id = u.id
ORDER BY c.campaign_id;

-- 4. If you want to DELETE the campaign with user_id=1 (be careful!)
-- Uncomment the line below ONLY if you're sure:
-- DELETE FROM campaigns WHERE campaign_id = 1;

-- 5. If you want to UPDATE the campaign to belong to a specific user:
-- First, find the user_id for taha@gmail.com:
-- SELECT id FROM users WHERE email = 'taha@gmail.com';
-- Then update (replace USER_ID_HERE with the actual ID):
-- UPDATE campaigns SET user_id = USER_ID_HERE WHERE campaign_id = 1;

-- 6. Verify the fix:
-- SELECT c.campaign_id, c.user_id, c.campaign_name, u.email 
-- FROM campaigns c
-- JOIN users u ON c.user_id = u.id;

-- 7. Check which campaigns each user should see (based on their user_id):
SELECT 
    u.id as user_id,
    u.email,
    COUNT(c.campaign_id) as campaign_count,
    STRING_AGG(c.campaign_name, ', ') as campaign_names
FROM users u
LEFT JOIN campaigns c ON u.id = c.user_id
GROUP BY u.id, u.email
ORDER BY u.id;

-- 8. If the campaign with user_id=1 should only belong to taha@gmail.com, 
--    and you want to DELETE it permanently, run:
-- DELETE FROM campaigns WHERE campaign_id = 1;

-- 9. If you want to see what happens when each user tries to fetch campaigns:
--    (This simulates what getAllCampaigns does for each user)
SELECT 'User 1 (taha@gmail.com)' as user_info, campaign_id, campaign_name 
FROM campaigns WHERE user_id = 1
UNION ALL
SELECT 'User 2 (tahamoulaa@gmail.com)' as user_info, campaign_id, campaign_name 
FROM campaigns WHERE user_id = 2
UNION ALL
SELECT 'User 3 (hassna74zaim@gmail.com)' as user_info, campaign_id, campaign_name 
FROM campaigns WHERE user_id = 3;

