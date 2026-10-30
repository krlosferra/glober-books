#!/usr/bin/env bash
# PreToolUse guard for Bash tool calls (see docs/sdd/security.md, SC-24 and SC-25).
# Blocks: hook bypasses, and AI attribution inside commit, tag and pull-request commands.
# Reads the tool call as JSON on stdin; exit code 2 blocks the call and shows stderr to the agent.
# Only the value of "command" is inspected (other fields such as file paths are not part of the command).
project_dir="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
pattern=$(cat "$project_dir/.githooks/attribution-pattern" 2>/dev/null)
input=$(cat)
# Extract the command string and undo the most common JSON escapes.
cmd=$(printf '%s' "$input" | tr '\n' ' ' | grep -oE '"command" *: *"([^"\\]|\\.)*"' | head -1 \
  | sed -E 's/^"command" *: *"//; s/"$//' \
  | sed -e 's/\\n/ /g' -e 's/\\t/ /g' -e 's/\\"/"/g' | LC_ALL=C tr -d '\200-\377')

block() { echo "guard-bash: blocked. $1 (see docs/sdd/git-workflow.md)" >&2; exit 2; }

# A git or gh invocation starts at the beginning of the command or after ; & | (
start='(^|[;&|(]) *'
if printf '%s' "$cmd" | grep -qE "${start}git( +[^ ;&|]+)* +(commit|push)( [^;&|]*)? (--no-verify|-n)( |\$|\")"; then
  block "Bypassing git hooks (--no-verify) is forbidden"
fi
if printf '%s' "$cmd" | grep -qE "${start}git( +[^ ;&|]+)* +-c +core\.hooksPath|${start}git +config +[^;&|]*core\.hooksPath +[^ ;&|\"]*" \
   && ! printf '%s' "$cmd" | grep -qE "${start}git +config +core\.hooksPath +\.githooks( |\$|;|&|\")"; then
  block "Changing core.hooksPath is forbidden (the only allowed form is: git config core.hooksPath .githooks)"
fi
if printf '%s' "$cmd" | grep -qE "${start}(git( +[^ ;&|]+)* +(commit|tag|notes)( |\$|\")|gh +(pr|issue|api)( |\$|\"))"; then
  scrubbed=$(printf '%s' "$cmd" | sed -E 's#\.claude/##g; s#CLAUDE\.md##g')
  if [ -n "$pattern" ] && printf '%s' "$scrubbed" | grep -qiE "$pattern"; then
    block "The command text reveals an AI tool, agent or co-author (commit, tag or pull-request text must not contain attribution)"
  fi
fi
exit 0
