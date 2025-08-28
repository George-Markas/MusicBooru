CREATE VIEW user_auth_view AS
SELECT
    id,
    username,
    password,
    role
FROM _user;