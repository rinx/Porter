(ns porter.app
  (:require [porter.state :as state]
            [porter.ui :as ui]
            [porter.http :as http]
            [om.next :as om :refer-macros [defui]]
            [clojure.string :as string]
            [cljs.core.async :as async
                             :refer [take!]]))

(set! js/window.React (js/require "react"))

(defn request-rss-url [url callback]
  (take! (http/get-parse-rss url) #(callback %)))

(defn update-all-script []
  (take! (http/fetch-all-script)
    (fn [scripts]
      (swap! state/app-state assoc :app/scripts scripts))))

(defn fetch-url [component url]
  (when-not (string/blank? url)
    (request-rss-url url
      #(ui/alert "Fetched\n"
                 "Title: " %))))

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
                          :onSubmitEditing #(fetch-url this @hidden-input)})
          (ui/touchable-highlight {:style {:flex 1
                                           :backgroundColor "#999"
                                           :padding 10
                                           :borderRadius 5}
                                   :onPress #(fetch-url this @hidden-input)}
            (ui/text {:style {:color "white"
                              :textAlign "center"
                              :fontWeight "bold"}}
                     "fetch")))))))

(def rss-input-page (om/factory RSSInputPage))

(defui TextInputPage
  Object
  (render [this]
    (let [hidden-title-input (atom "")
          hidden-body-input (atom "")]
      (ui/view {:style {:flexDirection "column"
                        :margin 0
                        :paddingLeft 20
                        :paddingRight 20
                        :alignItems "center"}}
        (ui/text {:style {:fontSize 30
                          :fontWeight "100"
                          :marginBottom 20
                          :textAlign "center"}}
                 "Input title and body")
        (ui/view {:style {:flexDirection "row"
                          :marginBottom 20}}
          (ui/text {:style {:marginRight 10
                            :padding 10
                            :textAlign "center"}}
                   "Title")
          (ui/text-input {:style {:flex 3
                                  :borderColor "gray"
                                  :borderWidth 1
                                  :borderRadius 5}
                          :onChangeText #(reset! hidden-title-input %)}))
        (ui/view {:style {:flexDirection "row"
                          :marginBottom 20}}
          (ui/text {:style {:marginRight 10
                            :padding 10
                            :textAlign "center"}}
                   "Body")
          (ui/text-input {:style {:flex 3
                                  :borderColor "gray"
                                  :borderWidth 1
                                  :borderRadius 5}
                          :onChangeText #(reset! hidden-body-input %)}))
        (ui/touchable-highlight {:style {:flex 1
                                         :backgroundColor "#999"
                                         :padding 10
                                         :borderRadius 5}
                                 :onPress #(when-not (or (string/blank? @hidden-title-input)
                                                         (string/blank? @hidden-body-input))
                                             (http/post-new-script @hidden-title-input @hidden-body-input)
                                             (ui/alert "POSTED!\n"
                                                       "Title: " @hidden-title-input "\n"
                                                       "Body: " @hidden-body-input))}
          (ui/text {:style {:color "white"
                            :textAlign "center"
                            :fontWeight "bold"}}
                   "Post"))))))

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
    (let [scripts (om/props this)
          scripts' (->> scripts
                     (sort-by :id)
                     (reverse))]
      (apply ui/view {:style {:flexDirection "column"
                              :marginTop 0
                              :paddingRight 20
                              :paddingLeft 20}}
             (map script-item scripts')))))

(def script-list-page (om/factory ScriptListPage))

(defui AppRoot
  static om/IQuery
  (query [this]
    (let [script-subquery (om/get-query ScriptItem)]
      `[{:app/scripts ~script-subquery}]))
  Object
  (render [this]
    (let [props (om/props this)
          {:keys [:app/scripts]} props]
      (ui/view {:style {:flex 1}}
        (ui/scrollable-tab-view {:style {:marginTop 20}
                                 :onChangeTab #(update-all-script)}
          (ui/scroll-view {:paddingTop 20
                           :marginBottom 80
                           :tabLabel "RSS"}
                          (rss-input-page))
          (ui/scroll-view {:paddingTop 20
                           :marginBottom 80
                           :tabLabel "TEXT"}
                          (text-input-page))
          (ui/scroll-view {:paddingTop 20
                           :marginBottom 80
                           :tabLabel "LIST"}
                          (script-list-page scripts)))
        (ui/player-item {:url ""})))))

