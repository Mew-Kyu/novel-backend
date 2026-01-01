-- Convert from many-to-many to one-to-many relationship
-- Each user will have only one role

-- Step 1: Add role_id column to users table
ALTER TABLE users ADD COLUMN role_id BIGINT;

-- Step 2: Migrate data - assign the first role (or USER role) to each user
-- Priority: ADMIN > MODERATOR > USER
UPDATE users u
SET role_id = (
    SELECT ur.role_id
    FROM user_roles ur
    INNER JOIN roles r ON ur.role_id = r.id
    WHERE ur.user_id = u.id
    ORDER BY
        CASE r.name
            WHEN 'ADMIN' THEN 1
            WHEN 'MODERATOR' THEN 2
            WHEN 'USER' THEN 3
            ELSE 4
        END
    LIMIT 1
);

-- Step 3: For any users without a role, assign USER role
UPDATE users u
SET role_id = (SELECT id FROM roles WHERE name = 'USER')
WHERE role_id IS NULL;

-- Step 4: Make role_id NOT NULL now that all users have a role
ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;

-- Step 5: Add foreign key constraint
ALTER TABLE users
    ADD CONSTRAINT fk_users_role
    FOREIGN KEY (role_id)
    REFERENCES roles(id);

-- Step 6: Create index for better performance
CREATE INDEX idx_users_role_id ON users(role_id);

-- Step 7: Drop the old user_roles junction table
DROP TABLE user_roles;

