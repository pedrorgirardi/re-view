(ns re-view.routing
  (:require [goog.events :as events]
            [goog.dom :as gdom]
            [clojure.string :as str])
  (:import
   [goog History]
   [goog.history Html5History]
   [goog Uri]))

(defn segments
  "Splits route into segments, ignoring leading and trailing slashes."
  [route]
  (let [segments (-> route
                     (str/replace #"[#?].*" "")
                     (str/split \/ -1))
        segments (cond-> segments
                         (= "" (first segments)) (subvec 1))]
    (cond-> segments
            (= "" (last segments)) (pop))))

(comment (assert (= (segments "/") []))
         (assert (= (segments "//") [""]))
         (assert (= (segments "///") ["" ""]))
         (assert (= (segments "/a/b")
                    (segments "a/b/")
                    (segments "a/b") ["a" "b"])))

(defn query
  "Returns query parameters as map."
  [path]
  (let [uri (cond-> path
                    (string? path) (Uri.))
        data (.getQueryData uri)]
    (reduce (fn [m k]
              (assoc m (keyword k) (.get data k))) {} (.getKeys data))))

(defn parse-path
  "Returns map of parsed location information for path."
  [path]
  (let [uri (Uri. path)]
    {:path path
     :segments (segments path)
     :query (query uri)
     :fragment (.getFragment uri)}))

;; From http://www.lispcast.com/mastering-client-side-routing-with-secretary-and-goog-history
;; Replaces this method: https://closure-library.googlecode.com/git-history/docs/local_closure_goog_history_html5history.js.source.html#line237
;; Without this patch, google closure does not handle changes to query parameters.
(set! (.. Html5History -prototype -getUrl_)
      (fn [token]
        (this-as this
          (if (.-useFragment_ this)
            (str "#" token)
            (str (.-pathPrefix_ this) token)))))

(def browser? (exists? js/window))
(def history-support? (when browser? (.isSupported Html5History)))

(defn get-route
  "In a browsing environment, reads the current location."
  []
  (if history-support?
    (str js/window.location.pathname js/window.location.search js/window.location.hash)
    (if (= js/window.location.pathname "/")
      (.substring js/window.location.hash 1)
      (str js/window.location.pathname js/window.location.search))))

(defn- make-history
  "Set up browser history, using HTML5 history if available."
  []
  (when browser?
    (if history-support?
      (doto (Html5History.)
        (.setPathPrefix (str js/window.location.protocol
                             "//"
                             js/window.location.host))
        (.setUseFragment false))
      (if (not= "/" js/window.location.pathname)
        (set! (.-location js/window) (str "/#" (get-route)))
        (History.)))))

(defonce history
         (some-> (make-history)
                 (doto (.setEnabled true))))

(defn nav!
  "Trigger pushstate navigation to token (path)"
  ([route] (nav! route true))
  ([route add-history-state?]
   (if add-history-state?
     (.setToken history route)
     (.replaceToken history route))))

(defn- remove-empty
  "Remove empty values/strings from map"
  [m]
  (reduce-kv (fn [m k v]
               (cond-> m
                       (or (nil? v)
                           (= "" v)) (dissoc k))) m m))

(defn query-string [m]
  (let [js-query (-> m
                     (remove-empty)
                     (clj->js))]
    (-> Uri .-QueryData (.createFromMap js-query) (.toString))))

(defn query-nav!
  [query]
  (let [query-string (query-string query)]
    (nav! (cond-> (.. js/window -location -pathname)
                  (not (#{nil ""} query-string)) (str "?" query-string)))))

(defn swap-query!
  [f & args]
  (query-nav! (apply f (query (get-route)) args)))

(defn link?
  "Return true if element is a link"
  [el]
  (some-> el .-tagName (= "A")))

(defn closest
  "Return element or first ancestor of element that matches predicate, like jQuery's .closest()."
  [el pred]
  (if (pred el)
    el
    (gdom/getAncestor el pred)))

(defn click-event-handler
  "Intercept clicks on links with valid pushstate hrefs. Callback is passed the link."
  [callback e]
  (when-let [link (closest (.-target e) link?)]
    (let [location ^js (.-location js/window)
          ;; in IE/Edge, link elements do not expose an `origin` attribute
          origin (str (.-protocol location)
                      "//"
                      (.-host location))
          ;; check to see if we should let the browser handle the link
          ;; (eg. external link, or valid hash reference to an element on the page)
          handle-natively? (or (not= (.-host location) (.-host link))
                               (not= (.-protocol location) (.-protocol link))
                               ;; if only the hash has changed, & element exists on page, allow browser to scroll there
                               (and (.-hash link)
                                    (= (.-pathname location) (.-pathname link))
                                    (not= (.-hash location) (.-hash link))
                                    (.getElementById js/document (subs (.-hash link) 1))))]
      (when-not handle-natively?
        (.preventDefault e)
        (callback (str/replace (.-href link) origin ""))))))

(def intercept-clicks
  "Intercept local links (handle with router instead of reloading page)."
  (memoize                                                  ;; only do this once per page
   (fn intercept
     ([]
      (when browser?
        (intercept js/document)))
     ([element]
      (when browser?
        (events/listen element "click" (partial click-event-handler nav!)))))))

(defn listen
  "Set up a listener on route changes. Options:

  intercept-clicks? (boolean, `true`): For `click` events on local links, prevent page reload & fire listener instead.
  fire-now? (boolean, `true`): executes listener immediately, with current parsed route.

  Returns a key which can be passed to `unlisten` to remove listener."
  ([listener]
   (listen listener {}))
  ([listener {:keys [fire-now? intercept-clicks?]
              :or {fire-now? true
                   intercept-clicks? true}}]
   (when intercept-clicks? (intercept-clicks))
   (when fire-now? (listener (parse-path (get-route))))
   (events/listen history "navigate" #(listener (parse-path (get-route))))))

(defn unlisten [k]
  (if (fn? k)
    (events/unlisten history "navigate" k)
    (events/unlistenByKey k)))

