(ns gregorstocks.ring-proxy
  (:require [clojure.string          :refer [split]]
            [clj-http.client         :refer [request]])
  (:import (java.net URI)))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn wrap-proxy
  "Proxies requests to proxied-path, a local URI, to the remote URI at
  remote-uri-base, also a string."
  [handler ^String proxied-path remote-uri-base & [http-opts]]

  (fn [req]
    (if (.startsWith ^String (:uri req) (str proxied-path "/"))
      (let [uri (URI. remote-uri-base)
            remote-uri (URI. (.getScheme uri)
                             (.getAuthority uri)
                             (str (.getPath uri)
                                  (subs (:uri req) (.length proxied-path)))
                             nil
                             nil)]
        (-> (merge {:method (:request-method req)
                    :url (str remote-uri "?" (:query-string req))
                    :headers (dissoc (:headers req) "host" "content-length")
                    :body (if-let [len (get-in req [:headers "content-length"])]
                            (slurp-binary (:body req) (Integer/parseInt len)))
                    :follow-redirects true
                    :throw-exceptions false
                    :as :stream} http-opts)
            request))
      (handler req))))
