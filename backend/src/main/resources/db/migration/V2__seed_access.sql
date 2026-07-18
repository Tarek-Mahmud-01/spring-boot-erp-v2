-- =====================================================================
-- V2 — Seed baseline access: an 'admin' role with all foundation permissions,
-- and a default admin user. The password hash below is BCrypt for the literal
-- password "admin123" — CHANGE IT after first login (dev bootstrap only).
-- ULIDs are fixed literals so the seed is deterministic and re-runnable-safe.
-- =====================================================================

-- Permissions for the pilot (settings + access) module.
insert into permissions (public_id, code, name, module, description) values
  ('01J0AA000000000000PERM01', 'access.user.read',   'View users',        'access', 'List and view users'),
  ('01J0AA000000000000PERM02', 'access.user.write',  'Manage users',      'access', 'Create, update, delete users'),
  ('01J0AA000000000000PERM03', 'access.role.read',   'View roles',        'access', 'List and view roles'),
  ('01J0AA000000000000PERM04', 'access.role.write',  'Manage roles',      'access', 'Create, update, delete roles'),
  ('01J0AA000000000000PERM05', 'settings.company.read',  'View company',  'settings', 'View company settings'),
  ('01J0AA000000000000PERM06', 'settings.company.write', 'Manage company','settings', 'Update company settings');

-- Admin role.
insert into roles (public_id, code, name, description, is_system) values
  ('01J0AA000000000000ROLE01', 'admin', 'Administrator', 'Full access to all modules', true);

-- Grant every seeded permission to admin.
insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
cross join permissions p
where r.code = 'admin';

-- Default admin user (password: admin123 — bcrypt, cost 10).
insert into users (public_id, username, email, full_name, password_hash, is_active) values
  ('01J0AA000000000000USER01', 'admin', 'admin@guru-erp.local', 'System Administrator',
   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true);

-- Assign admin role to admin user.
insert into user_roles (user_id, role_id)
select u.id, r.id
from users u, roles r
where u.username = 'admin' and r.code = 'admin';
