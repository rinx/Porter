(ns porter.http
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >! take! put!]
                                       :refer-macros [go]]
            [tubax.core :as tubax]
            [venia.core :as venia]))

(def graphql-endpoint "http://52.193.81.107/graphql")

(def fetch-all-script-query
  (venia/graphql-query
    {:venia/queries
     [[:allScript
       [:id
        :title
        :body
        :speech_url
        :updated_at
        :created_at]]]}))
(defn script-input-query
  [title body]
  (str "mutation"
    (venia/graphql-query
      {:venia/queries
       [[:createScript
         {:input
          {:scriptInput
           {:title title
            :body body}}}
         [[:script
           [:title
            :body]]]]]})))

(defn graphql-post [url venia-query]
  (http/post url {:json-params {:query venia-query}}))

(defn get-parse-rss
  [url]
  (let [ch (async/chan)]
    (go
      (let [res (<! (http/get url))
            body (:body res)
            parsed-body (if-not (string/blank? body)
                          (-> (tubax/xml->clj body)
                              (zip/xml-zip)
                              (zip/down)
                              (zip/down)
                              (first)
                              (:content)
                              (first)
                              (str)) ; temporarily get title of rss feed
                          body)]
        (>! ch parsed-body)))
    ch))

(defn fetch-all-script []
  (let [ch (async/chan)]
    (go
      (let [res (<! (graphql-post graphql-endpoint
                                  fetch-all-script-query))
            scripts (get-in res [:body :data :allScript])]
        (>! ch scripts)))
    ch))

(defn post-new-script [title body]
  (graphql-post graphql-endpoint
                (script-input-query title body)))

