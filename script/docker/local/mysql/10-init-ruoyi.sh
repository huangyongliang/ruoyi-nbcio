#!/bin/bash
set -e

echo "Initializing ruoyi-nbcio database..."
mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /tmp/ruoyi-nbcio-mysql5.7.sql

echo "Applying local demo fixes..."
mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" \
  -e "UPDATE sys_user SET avatar = '' WHERE user_name = 'admin';"

if [ -f /tmp/dingtalk_attendance.sql ]; then
  echo "Applying DingTalk attendance schema..."
  mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /tmp/dingtalk_attendance.sql
fi

if [ -f /tmp/financing_daily_detail.sql ]; then
  echo "Applying financing daily detail schema..."
  mysql --default-character-set=utf8mb4 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < /tmp/financing_daily_detail.sql
fi
