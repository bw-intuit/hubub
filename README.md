# hubub

Hubub is a Clojure library designed to manage your GitHub organizations users.

## Usage

hubub creates teams and associates users and repos with those teams based on a
hashmap that of users to repos.

Set the **HUBUB_GITHUB_TOKEN** environment variable with a token that has **admin:org** permissions
for a user who is a member of the org being managed.

```
export HUBUB_GITHUB_TOKEN=abcd1234
```

Clone down this repo and open a REPL

```
lein repl
```

Use this library

```
(use 'hubub.core)
```

Defin your user to repos mapping

```
(def input
  {"user1" {:repos ["repo1"]}
   "user2" {:repos ["repo1" "repo2"]}})
```

Execute run against given GitHub organization with this repo-mapping

```
(run "this-is-a-test-1234" input)
```

Will add perform the following

* Create teams repo1-contributors and repo2-contributors in organization this-is-a-test-1234
* Add user1 to repo1-contributors
* Add user2 to repo1-contributors and repo2-contributors
* Add grant repo1-contributors push access to repo1
* Add grant repo2-contributors push access to repo2

### Custom Validations

You may want to validate each user is active in your company, hubub allows you to pass
in a verification funtion which will be applied to each user. If the function return a
falsy value, the user is removed. For example

We create the following function to check if a user is valid.

```
(defn check [username user-data] (= username "user1"))
```

And pass it as a third parameter to run

```
(run "this-is-a-test-1234" input check)
```

Will limit the users add to only those which check validates as true "user1". You can
replace the logic in this function with a verification against your company's auth system system.

## License

Copyright © 2015 Eclipse
