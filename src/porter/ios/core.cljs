(ns porter.ios.core
  (:require [clojure.string :as string]
            [om.next :as om :refer-macros [defui]]
            [re-natal.support :as sup]
            [porter.state :as state]
            [cljs-http.client :as http]
            [cljs.core.async :as async :refer [<! >!]
                                       :refer-macros [go]]))

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

(defn http-get [url]
  (let [ch (async/chan)]
    (go
      (let [res (<! (http/get url))]
        (>! ch res)))
    ch))

(defn http-post [url text]
  (go
    (let [res (<! (http/post url
                             {:json-params {:text text}}))]
      (alert (str res)))))
(defn url-post [text]
  (http-post "https://posttestserver.com/post.php" text))

(defn submit-url [this url]
  (when-not (string/blank? url)
    (om/transact! this `[(urls/add ~{:url url})])
    (alert url)))

(defui AppRoot
  static om/IQuery
  (query [this]
         '[:app/msg])
  Object
  (render [this]
          (let [{:keys [urls]} (om/props this)
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
                               :onChangeText #(reset! hidden-input %)
                               :onSubmitEditing #(submit-url this @hidden-input)})
                  (touchable-highlight {:style {:backgroundColor "#999"
                                                :padding 10
                                                :borderRadius 5}
                                        :onPress #(submit-url this @hidden-input)}
                                       (text {:style {:color "white"
                                                      :textAlign "center"
                                                      :fontWeight "bold"}} "register"))))))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler AppRoot 1)
  (.registerComponent app-registry "Porter" (fn [] app-root)))
