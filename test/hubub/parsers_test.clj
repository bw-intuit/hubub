(ns hubub.parsers-test
  (:require [clojure.test :refer :all]
            [hubub.stubs :refer :all]
            [hubub.parsers :refer :all]))

(deftest verify-repo-users
  (testing "make sure filters proper users"
    (is (= (repo-users "repo1" input valid-user-stub-fn "contributor")
           ["github-user1" "github-user3"]))
    (is (= (repo-users "repo1" input valid-user-stub-fn "admin")
           ["github-user3"]))
    (is (= (repo-users "repo2" input valid-user-stub-fn "contributor")
           ["github-user3"]))
    (is (= (repo-users "repo2" input valid-user-stub-fn "admin")
           ["github-user3"]))))

(deftest parse-repos-to-user-test
  (testing "test parse-repos-to-user"
    (is (= (parse-repos-to-users input "admin")
           {"repo1" ["github-user2" "github-user3"]
            "repo2" ["github-user3"]}))
    (is (= (parse-repos-to-users input "contributor")
           {"repo1" ["github-user1" "github-user2" "github-user3"]
            "repo2" ["github-user2" "github-user3"]}))))
