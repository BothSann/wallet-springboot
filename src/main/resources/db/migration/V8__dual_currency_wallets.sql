-- 1. Move pin_hash from wallets to users
ALTER TABLE users
    ADD COLUMN pin_hash VARCHAR(255);

-- 2. Copy existing PINs from wallets to users (preserves existing data)
UPDATE users u
SET pin_hash = w.pin_hash
FROM wallets w
WHERE w.user_id = u.id;

-- 3. Drop pin_hash from wallets
ALTER TABLE wallets
    DROP COLUMN pin_hash;

-- 4. Drop the unique constraint on wallets.user_id
--    PostgreSQL auto-names inline UNIQUE constraints as: tablename_columnname_key
ALTER TABLE wallets
    DROP CONSTRAINT wallets_user_id_key;

-- 5. Add is_default column to wallets
ALTER TABLE wallets
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- 6. Mark all existing wallets as default (each user previously had exactly one wallet)
UPDATE wallets
SET is_default = TRUE;
