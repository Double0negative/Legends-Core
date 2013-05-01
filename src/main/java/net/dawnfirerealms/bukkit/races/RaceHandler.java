package net.dawnfirerealms.bukkit.races;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.dawnfirerealms.bukkit.DFPlugin;
import net.dawnfirerealms.bukkit.utils.ConfigHandler;
import net.dawnfirerealms.bukkit.utils.EventMethod;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.EventExecutor;

import se.ranzdo.bukkit.methodcommand.Arg;
import se.ranzdo.bukkit.methodcommand.Command;
import se.ranzdo.bukkit.methodcommand.CommandHandler;
import se.ranzdo.bukkit.methodcommand.FlagArg;
import se.ranzdo.bukkit.methodcommand.Flags;

public class RaceHandler {
	public static final String INSTANCE_PATH = "race/players/";
	public static final String CONFIG_PATH = "race/";
	
	private static Map<RaceType, Map<Class<? extends Event>, List<EventMethod>>> events = new HashMap<RaceType, Map<Class<? extends Event>, List<EventMethod>>>();
	private static Set<Class<? extends Event>> register = new LinkedHashSet<Class<? extends Event>>();
	
	static {
		for(RaceType type : RaceType.values()) {
			Map<Class<? extends Event>, List<EventMethod>> map = events.get(type);
			if(map == null) {
				map = new HashMap<Class<? extends Event>, List<EventMethod>>();
				events.put(type, map);
			}
			
			for(Entry<Class<? extends Event>, List<EventMethod>> entry : type.getEventMethods().entrySet()) {
				register.add(entry.getKey());
				map.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	private RaceListener listener = new RaceListener();
	private RaceEventExecutor executor = new RaceEventExecutor();
	private Map<RaceType, Set<Race>> classes = new HashMap<RaceType, Set<Race>>();
	private Map<Player, Race> players = new HashMap<Player, Race>();
	private ConfigHandler configHandler = DFPlugin.instance.getConfigHandler();
	
	public RaceHandler() {
		Bukkit.getPluginManager().registerEvents(listener, DFPlugin.instance);
		for(Class<? extends Event> event : register) {
			for(EventPriority prio : EventPriority.values()) {
				Bukkit.getPluginManager().registerEvent(event, new RaceEventExecutorListener(prio), prio, executor, DFPlugin.instance);
			}
		}
		CommandHandler chandler = DFPlugin.instance.getCommandHandler();
		chandler.registerArgumentHandler(RaceType.class, new RaceTypeArgumentHandler(this));
		chandler.registerCommands(new Commands());
		ConfigHandler configh = DFPlugin.instance.getConfigHandler();
		for(RaceType type : RaceType.values()) {
			configh.loadConfiguration(CONFIG_PATH + type.getRaceClass().getSimpleName(), type.getRaceClass());
		}
		for(Player player: Bukkit.getOnlinePlayers()) {
			loadRaceInstance(player);
		}
	}
	
	public Race getRace(Player player) {
		Race i = players.get(player);
		if(i == null)
			throw new IllegalStateException("Could not find a classinstance to player "+player.toString());
		
		return i;
	}
	
	public Set<Race> getAllOfRace(RaceType type) {
		Set<Race> pairs = classes.get(type);
		if(pairs == null)
			pairs = new LinkedHashSet<Race>();
		
		return pairs;
	}
	
	public void setRace(Player player, RaceType race) {
		try {
			Race before = getRace(player);
			before.uninit();
			classes.get(before.getType()).remove(before);
		}
		catch(IllegalStateException e) {}
		
		Race after = race.newInstance(player);
		setRace(player, after);
	}
	
	private void setRace(Player player, Race race) {
		race.handler = RaceHandler.this;
		race.configHandler = configHandler;
		players.put(player, race);
		Set<Race> pairs = classes.get(race);
		if(pairs == null) {
			pairs = new LinkedHashSet<Race>();
			classes.put(race.getType(), pairs);
		}
		pairs.add(race);
	}
	
	private class RaceEventExecutorListener implements Listener {
		private EventPriority prio;

		private RaceEventExecutorListener(EventPriority prio) {
			this.prio = prio;
		}
	}
	
	private class RaceEventExecutor implements EventExecutor {
		public void execute(Listener l, Event event) throws EventException {
			Player player;
			if(event instanceof PlayerEvent)
				player = ((PlayerEvent)event).getPlayer();
			else if(event instanceof EntityEvent) {
				EntityEvent e = (EntityEvent) event;
				if(e.getEntity() instanceof Player)
					player = (Player) e.getEntity();
				else
					return;
			}
			else
				return;
			
			Race instance = getRace(player);
			Map<Class<? extends Event>, List<EventMethod>> events = RaceHandler.events.get(instance.getType());
			List<EventMethod> list = events.get(event.getClass());
			if(list != null) {
				for(EventMethod method : list) {
					if(method.getPriority() != ((RaceEventExecutorListener)l).prio)
						continue;
					
					if(method.isIgnoreCancelled() && event instanceof Cancellable && ((Cancellable)event).isCancelled())
						continue;
					try {
						method.getMethod().invoke(instance, event);
					} catch (Exception e) {
						throw new EventException(e);
					}
				}
			}
		}
	}
	
	private void loadRaceInstance(Player player) {
		Race race = (Race) configHandler.loadInstance(INSTANCE_PATH + player.getName());
		if(race == null)
			return;
		race.player = player;
		setRace(player, race);
	}
	
	
	private class RaceListener implements Listener {
		@EventHandler(priority = EventPriority.LOWEST)
		public void onJoin(PlayerJoinEvent event) {
			loadRaceInstance(event.getPlayer());
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onQuit(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			Race i = getRace(player);
			i.uninit();
			players.remove(player);
			classes.get(i.getType()).remove(i);
		}
	}
	
	private class Commands {
		@Command(identifier = "race set", description = "Sets the race of yourself or another player.", permissions = "df.race.setrace")
		@Flags(identifier = "p", description = "the player")
		public void commandSetRace(
				CommandSender sender,
				@FlagArg("p") @Arg(name = "targetplayer", def = "?sender") Player player,
				@Arg(name = "racetype") RaceType type
		) {
			setRace(player, type.newInstance(player));
			sender.sendMessage("The race was changed to "+type.getColorizedName()+" for player "+player.getDisplayName());
		}
		
		@Command(identifier = "race list", description = "Gets a list of all online players and what race they are.")
		public void getListOfRace(
				CommandSender sender
		) {
			ArrayList<String> m = new ArrayList<String>();
			for(RaceType type : RaceType.values()) {
				m.add("==="+type.getColorizedName()+"===");
				for(Race race : getAllOfRace(type)) {
					m.add(race.getPlayer().getDisplayName());
				}
				m.add("");
			}
			sender.sendMessage(m.toArray(new String[0]));
		}
	}
}