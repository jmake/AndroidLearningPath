
### To update local main from remote: 
```
git fetch --all --prune

git switch main 
git reset --hard origin/main
git clean -fd
```

```
git switch other_branch
git reset --hard origin/other_branch
git clean -fd
```



