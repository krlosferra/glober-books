# Handoff 093: Task 14 delivery (smoke tests, guard fixture and README)
- From: frontend
- To: liaison
- Phase: 4   Round: 1
- Type: TASK_DELIVERY
- Task: 14, branch `chore/task-14-smoke-guard-readme` (local commits, not pushed)

## Resume point
Done: builders and library; fixtures with guard; smoke spec (3 green, about 9 s); guard negative control.
Next: README; progress note (0b commands, Task 1 proof table); clean-copy proof (build, smoke, jar start, POST and GET); tool-server attempt; final evidence (diff stat, log).

## Evidence
### Smoke run (desktop)
```
  3 passed (8.8s)   (M-1 keyboard add took 294 ms; tests 333, 578, 248 ms)
```
### Guard negative control (throwaway spec `zz-control.spec.ts`, deleted after the run; fixtures.ts restored with git checkout)
Seeded, each failed the guard with the quoted detail:
- inline script via a script element: `securitypolicyviolation events` = `require-trusted-types-for trusted-types-sink` (Trusted Types blocked the sink)
- fetch to `http://127.0.0.1:1/x`: violation `connect-src http://127.0.0.1:1/x` and the console CSP messages
- navigation to `http://127.0.0.1:1/x`: `requests to another origin` = `http://127.0.0.1:1/x`
- uncaught error in a timer: `page errors` = `seeded`
- inline style attribute and style element: `style-src-attr inline`, `style-src-elem inline`
- SEC-F-130: with the filter constant replaced by a dummy, all three smoke tests failed with `Error with Permissions-Policy header: Unrecognized feature: 'bluetooth'.` (exact text, so the filter constant is the real message); with the constant restored the smoke is green. The guard treats Permissions-Policy console messages as guarded, so only that exact string is tolerated.
- Not done: no seeded wrong-header control (the header check passes on every asset of the real page; a wrong value was not provoked).

