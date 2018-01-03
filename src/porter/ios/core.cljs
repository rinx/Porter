(ns porter.ios.core
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [om.next :as om :refer-macros [defui]]
            [reagent.core :as reagent]
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
(def ReactNativeScrollableTabView (js/require "react-native-scrollable-tab-view"))
(def ReactNativeAudioStreaming (js/require "react-native-audio-streaming"))

(defn create-element [rn-comp opts & children]
  (apply js/React.createElement rn-comp (clj->js opts) children))

(def app-registry (.-AppRegistry ReactNative))
(def view (partial create-element (.-View ReactNative)))
(def text (partial create-element (.-Text ReactNative)))
(def touchable-highlight (partial create-element (.-TouchableHighlight ReactNative)))
(def text-input (partial create-element (.-TextInput ReactNative)))
(def flat-list (partial create-element (.-FlatList ReactNative)))
(def list-item (partial create-element (.-ListItem ReactNative)))

(def scrollable-tab-view (partial create-element ReactNativeScrollableTabView))

(def AudioStreaming (.-ReactNativeAudioStreaming ReactNativeAudioStreaming))
(def player-item (partial create-element (.-Player ReactNativeAudioStreaming)))

(defn play-stream [url]
  (.play AudioStreaming url (clj->js {:showIniOSMediaCenter true})))

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

(defn update-all-script [component]
  (go
    (let [res (<! (graphql-post graphql-endpoint
                                fetch-all-script-query))
          scripts (get-in res [:body :data :allScript])]
      (om/transact! component `[(scripts/update ~{:scripts scripts}) :app/scripts]))))

(defn post-new-script [title body]
  (graphql-post graphql-endpoint
                (script-input-query title body))
  (alert "POSTED!"))

(defn submit-url [component url]
  (when-not (string/blank? url)
    (om/transact! component `[(urls/add ~{:url url})])
    (request-rss-url url
      #(post-new-script "[FROM iOS App] てすてす" %))))

(defui RSSInputPage
  Object
  (render [this]
    (let [hidden-input (atom "")]
      (view {:style {:flexDirection "column" :margin 0 :alignItems "center"}}
            (text {:style {:fontSize 30
                           :fontWeight "100"
                           :marginBottom 20
                           :textAlign "center"}} "Please input RSS URL")
            (view {:style {:flexDirection "row"
                           :marginLeft 20
                           :marginRight 20
                           :marginBottom 20}}
              (text-input {:style {:flex 3
                                   :borderColor "gray"
                                   :borderWidth 1
                                   :borderRadius 5
                                   :marginRight 10}
                           :onChangeText #(reset! hidden-input %)
                           :onSubmitEditing #(submit-url this @hidden-input)})
              (touchable-highlight {:style {:flex 1
                                            :backgroundColor "#999"
                                            :padding 10
                                            :borderRadius 5}
                                    :onPress #(submit-url this @hidden-input)}
                                   (text {:style {:color "white"
                                                  :textAlign "center"
                                                  :fontWeight "bold"}} "register")))))))

(def rss-input-page (om/factory RSSInputPage))

(defui TextInputPage
  Object
  (render [this]
    (let [hidden-input (atom "")]
      (view {:style {:flexDirection "column" :margin 0 :alignItems "center"}}
            (text {:style {:fontSize 30
                           :fontWeight "100"
                           :marginBottom 20
                           :textAlign "center"}} "Please input body")
            (view {:style {:flexDirection "row"
                           :marginLeft 20
                           :marginRight 20
                           :marginBottom 20}}
              (text-input {:style {:flex 3
                                   :borderColor "gray"
                                   :borderWidth 1
                                   :borderRadius 5
                                   :marginRight 10}
                           :onChangeText #(reset! hidden-input %)
                           :onSubmitEditing #(submit-url this @hidden-input)})
              (touchable-highlight {:style {:flex 1
                                            :backgroundColor "#999"
                                            :padding 10
                                            :borderRadius 5}
                                    :onPress #(submit-url this @hidden-input)}
                                   (text {:style {:color "white"
                                                  :textAlign "center"
                                                  :fontWeight "bold"}} "register")))))))

(def text-input-page (om/factory TextInputPage))

(defui ScriptItem
  static om/IQuery
  (query [this]
    [:id :title :speech_url])
  Object
  (render [this]
     (let [props (om/props this)]
       (touchable-highlight {:style {:backgroundColor "#999"
                                     :padding 10
                                     :borderRadius 5
                                     :marginBottom 10}
                             :onPress #(play-stream (:speech_url props))}
                            (text {:style {:color "white"
                                           :textAlign "center"
                                           :fontWeight "bold"}}
                                  (str (:id props)
                                       ": "
                                       (:title props)))))))

(def script-item (om/factory ScriptItem))

(defui ScriptListPage
  static om/IQuery
  (query [this]
    [(om/get-query ScriptItem)])
  Object
  (render [this]
    (let [props (om/props this)
          scripts (:app/scripts props)]
      (apply view {:style {:flexDirection "column" :margin 0 :alignItems "center"}}
             (map script-item scripts)))))

(def script-list-page (om/factory ScriptListPage))

(defui AppRoot
  static om/IQuery
  (query [this]
    (let [script-subquery (om/get-query ScriptListPage)]
      [{:app/scripts script-subquery}]))
  Object
  (render [this]
    (let [props (om/props this)
          nowplaying (:app/nowplaying props)]
      (view {:style {:flex 1}}
            (scrollable-tab-view {:style {:marginTop 20}
                                  :onChangeTab #(update-all-script this)}
                                 (view {:paddingTop 20
                                        :tabLabel "RSS"}
                                       (rss-input-page))
                                 (view {:paddingTop 20
                                        :tabLabel "TEXT"}
                                       (text-input-page))
                                 (view {:paddingTop 20
                                        :tabLabel "LIST"}
                                       (script-list-page props)))
            (player-item {:url (:speech_url (first (:app/scripts props)))})))))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler AppRoot 1)
  (.registerComponent app-registry "Porter" (fn [] app-root)))
