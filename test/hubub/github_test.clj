(ns hubub.github-test
  (:require [clojure.test :refer :all]
            [hubub.github :refer :all]
            [hubub.stubs :refer :all]))

(deftest verify-list-repos
  (testing "verify listing repos"
    (binding [hubub.github/gh-list-repos list-repos-stub-success]
      (is (= (list-repos "org") ["project1"])))))

(deftest team-exists-test
  (binding [hubub.github/gh-list-teams list-teams-stub-success]
    (testing "valid team returns true"
      (is (= (team-exists? "org" "Owners") true)))
    (testing "valid team returns false"
      (is (= (team-exists? "org" "invalid") false)))))

(deftest create-team-test
  (binding [hubub.github/gh-create-team create-team-stub-success]
    (testing "create team"
      (is (= (:name (create-team "org" "team1-push" "admin")) "team1-push")))))

(deftest list-repos-test
  (binding [hubub.github/gh-list-repos list-repos-stub-success]
    (testing "success"
      (is (= (list-repos "test") ["project1"])))))

(deftest lookup-team-id-test
  (binding [hubub.github/gh-list-teams list-teams-stub-success]
    (testing "returns the team id"
      (is (= (lookup-team-id "org" "Owners") 123))))
    (testing "returns nil for unknown team"
      (is (nil? (lookup-team-id "org" "blah")))))

(deftest associate-repo-with-team-test
  (binding [hubub.github/gh-list-teams list-teams-stub-success]

    (testing "repo already associated"
      (binding [hubub.github/gh-team-associated-with-repo? (fn [team-id org repo-name]
                                                             (and (= org "org")
                                                                  (= repo-name "repo")
                                                                  (= team-id 123)))]

      (is (true? (associate-repo-with-team "org" "repo" "Owners")))))

    (testing "repo not associated and then succfully associated"
      (binding [hubub.github/gh-team-associated-with-repo? (fn [team-id org repo-name] false)
                hubub.github/gh-add-team-to-repo (fn [team-id org repo-name] (and
                                                                               (= org "org")
                                                                               (= repo-name "repo")
                                                                               (= team-id 123)))]
        (is (true? (associate-repo-with-team "org" "repo" "Owners")))))

    (testing "repo not associated and then filed to associate"
      (binding [hubub.github/gh-team-associated-with-repo? (fn [team-id org repo-name] false)
                hubub.github/gh-add-team-to-repo (fn [team-id org repo-name] false)]
        (is (false? (associate-repo-with-team "org" "repo" "Owners")))))))

(deftest user-member-of-team-pending-test
  (testing "is pending"
    (binding [hubub.github/gh-team-membership team-membership-pending]
      (is (true? (user-member-of-team-pending? "123" "bw-intuit")))))
  (testing "is active"
    (binding [hubub.github/gh-team-membership team-membership-active]
      (is (false? (user-member-of-team-pending? "123" "bw-intuit"))))))
