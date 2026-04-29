INSERT INTO mysql.global_priv (Host,User,Priv) VALUES
('localhost','root','{"access":18446744073709551615,"plugin":"mysql_native_password","authentication_string":""}'),
('127.0.0.1','root','{"access":18446744073709551615,"plugin":"mysql_native_password","authentication_string":""}'),
('localhost','pma','{"access":18446744073709551615,"plugin":"mysql_native_password","authentication_string":""}'),
('127.0.0.1','pma','{"access":18446744073709551615,"plugin":"mysql_native_password","authentication_string":""}');
SELECT Host,User FROM mysql.global_priv ORDER BY User,Host;
