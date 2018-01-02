(ns porter.ios.core
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [om.next :as om :refer-macros [defui]]
            [re-natal.support :as sup]
            [porter.state :as state]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >!]
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

(set! js/window.React (js/require "react"))
(def ReactNative (js/require "react-native"))

(defn create-element [rn-comp opts & children]
  (apply js/React.createElement rn-comp (clj->js opts) children))

(def app-registry (.-AppRegistry ReactNative))
(def view (partial create-element (.-View ReactNative)))
(def text (partial create-element (.-Text ReactNative)))
(def touchable-highlight (partial create-element (.-TouchableHighlight ReactNative)))
(def text-input (partial create-element (.-TextInput ReactNative)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn request-rss-url [url callback]
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
      (callback parsed-body))))

(defn graphql-post [url venia-query]
  (http/post url {:json-params {:query venia-query}}))

(defn fetch-all-script []
  (graphql-post graphql-endpoint
                fetch-all-script-query))

(defn post-new-script [title body]
  (graphql-post graphql-endpoint
                (script-input-query title body))
  (alert "POSTED!"))

(defn submit-url [this url]
  (when-not (string/blank? url)
    (om/transact! this `[(urls/add ~{:url url})])
    (request-rss-url url
      #(post-new-script "[FROM iOS App] てすてす" %))))




(defui AppRoot
  static om/IQuery
  (query [this]
         '[:urls])
  Object
  (render [this]
          (let [{:keys [urls]} (om/props this)
                hidden-input-rss-url (atom "")
                hidden-input (atom "")]
            (view {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
                  (text {:style {:fontSize 30
                                 :fontWeight "100"
                                 :marginBottom 20
                                 :textAlign "center"}} "Please input RSS URL")
                  (text-input {:style {:height 40
                                       :width 200
                                       :borderColor "gray"
                                       :borderWidth 1
                                       :marginBottom 20}
                               :onChangeText #(reset! hidden-input-rss-url %)
                               :onSubmitEditing #(submit-url this @hidden-input-rss-url)})
                  (touchable-highlight {:style {:backgroundColor "#999"
                                                :padding 10
                                                :borderRadius 5}
                                        :onPress #(submit-url this @hidden-input-rss-url)}
                                       (text {:style {:color "white"
                                                      :textAlign "center"
                                                      :fontWeight "bold"}} "register"))
                  (text {:style {:fontSize 30
                                 :fontWeight "100"
                                 :marginBottom 20
                                 :textAlign "center"}} "Please input body")
                  (text-input {:style {:height 40
                                       :width 200
                                       :borderColor "gray"
                                       :borderWidth 1
                                       :marginBottom 20}
                               :onChangeText #(reset! hidden-input %)
                               :onSubmitEditing #(post-new-script "[From iOS APP]" @hidden-input)})
                  (touchable-highlight {:style {:backgroundColor "#999"
                                                :padding 10
                                                :borderRadius 5}
                                        :onPress #(post-new-script "[From iOS App]" @hidden-input)}
                                       (text {:style {:color "white"
                                                      :textAlign "center"
                                                      :fontWeight "bold"}} "register"))))))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler AppRoot 1)
  (.registerComponent app-registry "Porter" (fn [] app-root)))
