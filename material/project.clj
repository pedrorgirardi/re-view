(defproject re-view/material "0.2.0-SNAPSHOT"

  :description "Material design components in re-view"

  :url "https://www.github.com/braintripping/re-view/tree/master/re_view_material"

  :license {:name "MIT License"
            :url  "http://www.opensource.org/licenses/mit-license.php"}

  :min-lein-version "2.7.1"


  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]]

  :profiles {:provided {:dependencies [[re-view "0.3.18"]]}
             :dev      {:dependencies [[org.clojure/test.check "0.9.0"]]}}

  :cljsbuild {:builds []}

  :lein-release {:deploy-via :clojars
                 :scm        :git}

  :source-paths ["src" "example"])