# hubub

Hubub is a Clojure library designed to manage your GitHub organizations users.

## Overview

As our GitHub organization grew, we experienced difficulties in ensuring that only
current employees had access to repos for which they had been assigned contributor
rights.

Hubub is a library we created to manage our GitHub users and validate they are employees
with rights to specific repos.

Hubub performs the following:

* Assigns GitHub usernames to specific repos in a GitHub organization.
* Maps GitHub usernames to your companies data.
* Perform validation against that data (i.g. ensure user is still active at company).
* Removes or adds users to repos based on the results of the above validations.

## Usage

Hubub creates teams and associates users and repos with those teams based on a
hashmap that of users to repos.

Clone down this repo and open a REPL

```
lein repl
```

Use this library

```
(use 'hubub.core)
```

Set the GitHub [token](https://help.github.com/articles/creating-an-access-token-for-command-line-use/) that has **admin:org** permissions for a user who is a
member of the org being managed.

```
(def token "abcd1234")
```

Define your user to repos mapping

```
(def input
  {"user1" {:repos ["repo1"]}
   "user2" {:repos ["repo1" "repo2"]}})
```

Execute run against given GitHub organization with this repo-mapping

```
(run "this-is-a-test-1234" input token)
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
(run "this-is-a-test-1234" input token check)
```

Will limit the users add to only those which check validates as true "user1". You can
replace the logic in this function with a verification against your company's auth system system.

## License

Copyright © 2015 Eclipse
