#!/bin/bash
npm i
bundle exec jekyll s --host 0.0.0.0 --source src/main/content --config src/main/content/_config.yml,src/main/content/_dev_config.yml --drafts