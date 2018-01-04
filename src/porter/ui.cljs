(ns porter.ui)

(def ReactNative (js/require "react-native"))
(def ReactNativeScrollableTabView (js/require "react-native-scrollable-tab-view"))
(def ReactNativeAudioStreaming (js/require "react-native-audio-streaming"))

(defn create-element [rn-comp opts & children]
  (apply js/React.createElement rn-comp (clj->js opts) children))

(def app-registry (.-AppRegistry ReactNative))
(def view (partial create-element (.-View ReactNative)))
(def scroll-view (partial create-element (.-ScrollView ReactNative)))
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

