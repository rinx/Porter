(ns porter.ios.core
  (:require [clojure.string :as string]
            [om.next :as om :refer-macros [defui]]
            [re-natal.support :as sup]
            [porter.state :as state]
            [porter.ui :as ui]
            [porter.http :as http]
            [cljs.core.async :as async
                             :refer [<! >! take! put!]
                             :refer-macros [go]]
            [tubax.core :as tubax]))

(set! js/window.React (js/require "react"))

(defn request-rss-url [url callback]
  (take! (http/get-parse-rss) #(callback %)))

(defn update-all-script [component]
  (take! (http/fetch-all-script)
    (fn [scripts]
      (om/transact! component `[(scripts/update ~{:scripts scripts}) :app/scripts]))))

(defn submit-url [component url]
  (when-not (string/blank? url)
    #_(om/transact! component `[(urls/add ~{:url url})])
    (request-rss-url url
      #(http/post-new-script "[FROM iOS App] てすてす" %))))

(defui RSSInputPage
  Object
  (render [this]
    (let [hidden-input (atom "")]
      (ui/view {:style {:flexDirection "column" :margin 0 :alignItems "center"}}
        (ui/text {:style {:fontSize 30
                          :fontWeight "100"
                          :marginBottom 20
                          :textAlign "center"}}
                 "Please input RSS URL")
        (ui/view {:style {:flexDirection "row"
                          :marginLeft 20
                          :marginRight 20
                          :marginBottom 20}}
          (ui/text-input {:style {:flex 3
                                  :borderColor "gray"
                                  :borderWidth 1
                                  :borderRadius 5
                                  :marginRight 10}
                          :onChangeText #(reset! hidden-input %)
                          :onSubmitEditing #(submit-url this @hidden-input)})
          (ui/touchable-highlight {:style {:flex 1
                                           :backgroundColor "#999"
                                           :padding 10
                                           :borderRadius 5}
                                   :onPress #(submit-url this @hidden-input)}
            (ui/text {:style {:color "white"
                              :textAlign "center"
                              :fontWeight "bold"}}
                     "register")))))))

(def rss-input-page (om/factory RSSInputPage))

(defui TextInputPage
  Object
  (render [this]
    (let [hidden-input (atom "")]
      (ui/view {:style {:flexDirection "column" :margin 0 :alignItems "center"}}
        (ui/text {:style {:fontSize 30
                          :fontWeight "100"
                          :marginBottom 20
                          :textAlign "center"}} "Please input body")
        (ui/view {:style {:flexDirection "row"
                          :marginLeft 20
                          :marginRight 20
                          :marginBottom 20}}
          (ui/text-input {:style {:flex 3
                                  :borderColor "gray"
                                  :borderWidth 1
                                  :borderRadius 5
                                  :marginRight 10}
                          :onChangeText #(reset! hidden-input %)
                          :onSubmitEditing #(submit-url this @hidden-input)})
          (ui/touchable-highlight {:style {:flex 1
                                           :backgroundColor "#999"
                                           :padding 10
                                           :borderRadius 5}
                                   :onPress #(submit-url this @hidden-input)}
            (ui/text {:style {:color "white"
                              :textAlign "center"
                              :fontWeight "bold"}}
                     "register")))))))

(def text-input-page (om/factory TextInputPage))

(defui ScriptItem
  static om/Ident
  (ident [this {:keys [id]}]
    [:script/by-id id])
  static om/IQuery
  (query [this]
    '[:id :title :speech_url])
  Object
  (render [this]
     (let [props (om/props this)]
       (ui/touchable-highlight {:style {:backgroundColor "#999"
                                        :padding 10
                                        :borderRadius 5
                                        :marginBottom 10}
                                :onPress #(ui/play-stream (:speech_url props))}
         (ui/text {:style {:color "white"
                           :textAlign "center"
                           :fontWeight "bold"}}
                  (str (:id props)
                       ": "
                       (:title props)))))))

(def script-item (om/factory ScriptItem {:keyfn :id}))

(defui ScriptListPage
  Object
  (render [this]
    (let [scripts (om/props this)]
      (apply ui/scroll-view {:style {:flexDirection "column"
                                     :marginTop 0
                                     :marginRight 20
                                     :marginLeft 20
                                     :marginBottom 80}}
             (map script-item scripts)))))

(def script-list-page (om/factory ScriptListPage))

(defui AppRoot
  static om/IQuery
  (query [this]
    (let [script-subquery (om/get-query ScriptItem)]
      `[{:app/scripts ~script-subquery}]))
  Object
  (render [this]
    (let [props (om/props this)
          nowplaying (:app/nowplaying props)]
      (ui/view {:style {:flex 1}}
        (ui/scrollable-tab-view {:style {:marginTop 20}
                                 :onChangeTab #(update-all-script this)}
          (ui/view {:paddingTop 20
                    :tabLabel "RSS"}
                   (rss-input-page))
          (ui/view {:paddingTop 20
                    :tabLabel "TEXT"}
                   (text-input-page))
          (ui/view {:paddingTop 20
                    :tabLabel "LIST"}
                   (script-list-page (:app/scripts props))))
        (ui/player-item {:url (:speech_url (first (:app/scripts props)))})))))

(defonce RootNode (sup/root-node! 1))
(defonce app-root (om/factory RootNode))

(defn init []
  (om/add-root! state/reconciler AppRoot 1)
  (.registerComponent ui/app-registry "Porter" (fn [] app-root)))
