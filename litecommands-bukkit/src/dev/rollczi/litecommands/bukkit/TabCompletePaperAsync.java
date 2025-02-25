package dev.rollczi.litecommands.bukkit;

import dev.rollczi.litecommands.LiteCommandsException;
import dev.rollczi.litecommands.reflect.ReflectUtil;
import dev.rollczi.litecommands.suggestion.Suggestion;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class TabCompletePaperAsync extends TabComplete implements Listener {

    public TabCompletePaperAsync(Plugin plugin) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvent(PaperAsyncTabCompleteEvent.TAB_COMPLETE_CLASS, this, EventPriority.HIGHEST, (listener, event) -> executeListener(event), plugin);
    }

    private void executeListener(Event event) {
        PaperAsyncTabCompleteEvent tabCompleteEvent = new PaperAsyncTabCompleteEvent(event);
        Set<Suggestion> result = this.callListener(tabCompleteEvent.getSender(), tabCompleteEvent.getBuffer());

        if (result == null) {
            return;
        }

        tabCompleteEvent.completions(result);
    }

    public void close() {
        super.close();
        HandlerList.unregisterAll(this);
    }

    static class PaperAsyncTabCompleteEvent {

        private static final String TAB_COMPLETE_CLASS_NAME = "com.destroystokyo.paper.event.server.AsyncTabCompleteEvent";
        private static final String TAB_COMPLETION_CLASS_NAME = "com.destroystokyo.paper.event.server.AsyncTabCompleteEvent$Completion";
        private static final String COMPONENT_CLASS_NAME = "net{}kyori{}adventure{}text{}Component";// Ignore relocation

        static final Class<? extends Event> TAB_COMPLETE_CLASS;
        static final Class<?> TAB_COMPLETION_CLASS;
        static final Class<?> COMPONENT_CLASS;
        static final Method GET_COMMAND_SENDER_METHOD;
        static final Method GET_BUFFER_METHOD;
        static final Method SET_COMPLETIONS_METHOD;

        static final Method CREATE_COMPLETION_TOOLTIP;
        static final Method COMPLETIONS_METHOD;
        static final Method COMPONENT_TEXT_METHOD;

        static {
            TAB_COMPLETE_CLASS = ReflectUtil.getClass(TAB_COMPLETE_CLASS_NAME);
            GET_COMMAND_SENDER_METHOD = ReflectUtil.getMethod(TAB_COMPLETE_CLASS, "getSender");
            GET_BUFFER_METHOD = ReflectUtil.getMethod(TAB_COMPLETE_CLASS, "getBuffer");
            SET_COMPLETIONS_METHOD = ReflectUtil.getMethod(TAB_COMPLETE_CLASS, "setCompletions", List.class);

            TAB_COMPLETION_CLASS = ReflectUtil.getClass(TAB_COMPLETION_CLASS_NAME);
            COMPONENT_CLASS = ReflectUtil.getClass(COMPONENT_CLASS_NAME.replace("{}", "."));
            CREATE_COMPLETION_TOOLTIP = ReflectUtil.getMethod(TAB_COMPLETION_CLASS, "completion", String.class, COMPONENT_CLASS);
            COMPLETIONS_METHOD = ReflectUtil.getMethod(TAB_COMPLETE_CLASS, "completions", List.class);
            COMPONENT_TEXT_METHOD = ReflectUtil.getMethod(COMPONENT_CLASS, "text", String.class);
        }

        private final Object event;

        PaperAsyncTabCompleteEvent(Object event) {
            if (!ReflectUtil.instanceOf(event, TAB_COMPLETE_CLASS)) {
                throw new LiteCommandsException("Event is not instance of " + TAB_COMPLETE_CLASS_NAME);
            }

            this.event = event;
        }

        public CommandSender getSender() {
            return ReflectUtil.invokeMethod(GET_COMMAND_SENDER_METHOD, event);
        }

        public String getBuffer() {
            return ReflectUtil.invokeMethod(GET_BUFFER_METHOD, event);
        }

        public void setCompletions(List<String> completions) {
            ReflectUtil.invokeMethod(SET_COMPLETIONS_METHOD, event, completions);
        }

        public void completions(Set<Suggestion> suggestions) {
            List<Object> completions = suggestions.stream()
                .map(suggestion -> ReflectUtil.invokeMethod(CREATE_COMPLETION_TOOLTIP, null, suggestion.multilevel(), toComponent(suggestion.tooltip())))
                .collect(Collectors.toList());

            ReflectUtil.invokeMethod(COMPLETIONS_METHOD, event, completions);
        }

        public Object toComponent(String text) {
            if (text == null || text.isEmpty()) {
                return null;
            }

            // TODO ComponentSerializer support for Adventure
            // There we can have problems with relocation for adventure platform
            // Maybe we should use json format?
            return ReflectUtil.invokeMethod(COMPONENT_TEXT_METHOD, null, text);
        }
    }
}
