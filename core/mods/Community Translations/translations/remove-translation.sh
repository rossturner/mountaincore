#!/bin/bash

export PATTERN=$1

echo "removing $1"

find . -type f -name "*.csv" -exec sh -c '
	for file do

		echo "$file"
		sed -i "/$PATTERN/d" "$file"
	done
' exec-sh {} ';'
