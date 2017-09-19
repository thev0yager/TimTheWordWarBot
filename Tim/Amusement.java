package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import Tim.UserCommands.Amusement.Fridge;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
class Amusement {

	private final long timeout = 3000;

	private final List<String> pendingItems = new ArrayList<>();
	private final List<String> commandments = new ArrayList<>();
	private final List<String> flavours = new ArrayList<>();
	private final List<String> deities = new ArrayList<>();

	List<String> approvedItems = new ArrayList<>();
	List<String> ponderingList = new ArrayList<>();

	private ChannelInfo cdata;

	/**
	 * Parses user-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param event The event to parse
	 *
	 * @return True if message was handled, false otherwise.
	 */
	boolean parseUserCommand(MessageEvent event) {
		if (event.getUser() == null) {
			return false;
		}

		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
		String[] args = null;
		String argStr = "";

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			args = message.substring(space + 1).split(" ", 0);
			argStr = StringUtils.join(args, " ");
		} else {
			command = message.substring(1).toLowerCase();
		}

		command = command.replaceAll("\\W", "");
		cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
		switch (command) {
			case "attack":
				if (cdata.commands_enabled.get("attack")) {
					if (args != null && args.length > 0) {
						StringBuilder target = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							target.append(" ").append(args[i]);
						}
						attackCommand(event.getChannel(), event.getUser(), target.toString());
					} else {
						attackCommand(event.getChannel(), event.getUser(), null);
					}
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "banish":
				if (cdata.commands_enabled.get("banish")) {
					banish(event.getChannel(), args, true);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "catch":
				if (cdata.commands_enabled.get("catch")) {
					catchCommand(event.getChannel(), args);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "commandment":
				if (cdata.commands_enabled.get("commandment")) {
					commandment(event.getChannel(), args);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "dance":
				if (cdata.commands_enabled.get("dance")) {
					dance(event.getChannel());
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "defenestrate":
				if (cdata.commands_enabled.get("defenestrate")) {
					defenestrate(event.getChannel(), event.getUser(), args, true);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "eightball":
				if (cdata.commands_enabled.get("eightball")) {
					eightball(event.getChannel(), event.getUser(), false, argStr);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "expound":
				if (cdata.commands_enabled.get("expound")) {
					int which = Tim.rand.nextInt(3);
					String type = "say";
					if (which == 1 || (which == 2 && !argStr.equals(""))) {
						type = "mutter";
					} else if (which == 2) {
						type = "emote";
					}

					Tim.markov.randomAction(event.getChannel().getName(), type, argStr);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "foof":
				if (cdata.commands_enabled.get("foof")) {
					foof(event.getChannel(), event.getUser(), args, true);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "fridge":
				Fridge.parseCommand(args, event);
				return true;
			case "get":
				if (cdata.commands_enabled.get("get")) {
					getItem(event.getChannel(), event.getUser().getNick(), args);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "getfor":
				if (args != null && args.length > 0) {
					if (cdata.commands_enabled.get("get")) {
						if (args.length > 1) {
							// Want a new args array less the first old element.
							String[] newargs = new String[args.length - 1];
							System.arraycopy(args, 1, newargs, 0, args.length - 1);
							getItem(event.getChannel(), args[0], newargs);
						} else {
							getItem(event.getChannel(), args[0], null);
						}
					} else {
						event.respond("I'm sorry. I don't do that here.");
					}

					return true;
				}
				break;
			case "getfrom":
				if (args != null && args.length > 0) {
					if (cdata.commands_enabled.get("get")) {
						if (args.length > 1) {
							// Want a new args array less the first old element.
							String[] newargs = new String[args.length - 1];
							System.arraycopy(args, 1, newargs, 0, args.length - 1);
							getItemFrom(event.getChannel(), event.getUser().getNick(), args[0], newargs);
						} else {
							getItemFrom(event.getChannel(), event.getUser().getNick(), args[0], null);
						}
					} else {
						event.respond("I'm sorry. I don't do that here.");
					}

					return true;
				}
				break;
			case "herd":
				if (cdata.commands_enabled.get("herd")) {
					herd(event.getChannel(), event.getUser(), args);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "lick":
				if (cdata.commands_enabled.get("lick")) {
					lick(event, args);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "ping":
				if (cdata.commands_enabled.get("ping")) {
					if (Tim.rand.nextInt(100) < 80) {
						event.respond("Pong!");
					} else {
						event.getChannel().send().action("dives for the ball, but misses, and lands on a " + Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size())) + " couch.");
					}
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "raptorstats":
				if (cdata.commands_enabled.get("velociraptor")) {
					event.respond(String.format("There have been %d velociraptor sightings in this channel, and %d are still here. The last one was on %s.", cdata.velociraptorSightings, cdata.activeVelociraptors, cdata.getLastSighting()));
					event.respond(String.format("%d velociraptors in this channel have been killed by other swarms.", cdata.deadVelociraptors));
					event.respond(String.format("Swarms from this channel have killed %d other velociraptors.", cdata.killedVelociraptors));
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "search":
				if (cdata.commands_enabled.get("search")) {
					if (args != null && args.length > 0) {
						StringBuilder target = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							target.append(" ").append(args[i]);
						}
						search(event.getChannel(), event.getUser(), target.toString());
					} else {
						search(event.getChannel(), event.getUser(), null);
					}
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "sing":
				if (cdata.commands_enabled.get("sing")) {
					sing(event.getChannel());
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "summon":
				if (cdata.commands_enabled.get("summon")) {
					summon(event.getChannel(), args, true);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "woot":
				if (cdata.commands_enabled.get("woot")) {
					event.getChannel().send().action("cheers! Hooray!");
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			default:
				if ((!command.equals("")) && command.charAt(0) == 'd' && Pattern.matches("d\\d+", command)) {
					if (cdata.commands_enabled.get("dice")) {
						dice(command.substring(1), event);
					} else {
						event.respond("I'm sorry. I don't do that here.");
					}

					return true;
				}
				break;
		}

		return false;
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param event Event to process
	 *
	 * @return True if message was handled, false otherwise
	 */
	boolean parseAdminCommand(MessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
		String argsString;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 1);
		} else {
			command = message.toLowerCase().substring(1);
		}
		switch (command) {
			case "listitems": {
				int pages = (this.approvedItems.size() + 9) / 10;
				int wantPage = 0;
				if (args != null && args.length > 0) {
					try {
						wantPage = Integer.parseInt(args[0]) - 1;
					} catch (NumberFormatException ex) {
						event.respond("Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				event.respond(String.format("Showing page %d of %d (%d items total)", wantPage + 1, pages, this.approvedItems.size()));
				for (int i = 0; i < 10 && list_idx < this.approvedItems.size(); ++i, list_idx = wantPage * 10 + i) {
					event.respond(String.format("%d: %s", list_idx, this.approvedItems.get(list_idx)));
				}
				return true;
			}
			case "listpending": {
				int pages = (this.pendingItems.size() + 9) / 10;
				int wantPage = 0;
				if (args != null && args.length > 0) {
					try {
						wantPage = Integer.parseInt(args[0]) - 1;
					} catch (NumberFormatException ex) {
						event.respond("Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				event.respond(String.format("Showing page %d of %d (%d items total)", wantPage + 1, pages, this.pendingItems.size()));
				for (int i = 0; i < 10 && list_idx < this.pendingItems.size(); ++i, list_idx = wantPage * 10 + i) {
					event.respond(String.format("%d: %s", list_idx, this.pendingItems.get(list_idx)));
				}
				return true;
			}
			case "approveitem":
				if (args != null && args.length > 0) {
					int idx;
					StringBuilder item;
					try {
						idx = Integer.parseInt(args[0]);
						item = new StringBuilder(this.pendingItems.get(idx));
					} catch (NumberFormatException ex) {
						// Must be a string
						item = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							item.append(" ").append(args[i]);
						}
						idx = this.pendingItems.indexOf(item.toString());
					}
					if (idx >= 0) {
						this.setItemApproved(item.toString(), true);
						this.pendingItems.remove(idx);
						this.approvedItems.add(item.toString());
						event.respond(String.format("Item %s approved.", args[0]));
					} else {
						event.respond(String.format("Item %s is not pending approval.", args[0]));
					}
				}
				return true;
			case "disapproveitem":
				if (args != null && args.length > 0) {
					int idx;
					StringBuilder item;
					try {
						idx = Integer.parseInt(args[0]);
						item = new StringBuilder(this.approvedItems.get(idx));
					} catch (NumberFormatException ex) {
						// Must be a string
						item = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							item.append(" ").append(args[i]);
						}
						idx = this.approvedItems.indexOf(item.toString());
					}
					if (idx >= 0) {
						this.setItemApproved(item.toString(), false);
						this.pendingItems.add(item.toString());
						this.approvedItems.remove(idx);
						event.respond(String.format("Item %s disapproved.", args[0]));
					} else {
						event.respond(String.format("Item %s is not in approved list.", args[0]));
					}
				}
				return true;
			case "deleteitem":
				if (args != null && args.length > 0) {
					int idx;
					StringBuilder item;
					try {
						idx = Integer.parseInt(args[0]);
						item = new StringBuilder(this.pendingItems.get(idx));
					} catch (NumberFormatException ex) {
						// Must be a string
						item = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							item.append(" ").append(args[i]);
						}
						idx = this.pendingItems.indexOf(item.toString());
					}
					if (idx >= 0) {
						this.removeItem(item.toString());
						this.pendingItems.remove(item.toString());
						event.respond(String.format("Item %s has been deleted from pending list.", args[0]));
					} else {
						event.respond(String.format("Item %s is not pending approval.", args[0]));
					}
				}
				return true;
		}

		return false;
	}

	void helpSection(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String[] strs = {"Amusement Commands:",
						 "    !get <anything> - I will fetch you whatever you like.",
						 "    !getfor <someone> <anything> - I will give someone whatever you like.",
						 "    !eightball <your question> - I can tell you (with some degree of inaccuracy) how likely something is.",
						 "    !raptorstats - Details about this channel's raptor activity.",};

		for (String str : strs) {
			event.getUser().send().notice(str);
		}
	}

	void refreshDbLists() {
		this.getAypwipList();
		this.getCommandmentList();
		this.getDeityList();
		this.getFlavourList();
		this.getApprovedItems();
		this.getPendingItems();
	}

	void randomAction(User sender, String channel) {
		cdata = Tim.db.channel_data.get(channel);

		String[] actions = new String[]{
			"get", "eightball", "fridge", "defenestrate", "sing", "foof", "dance", "summon", "search", "herd", "banish", "catch"
		};

		if (sender == null) {
			HashSet<User> finalUsers = new HashSet<>(10);

			Collection<User> users = Tim.db.channel_data.get(channel).userList.values();

			int size = users.size();
			users.stream().filter((user) -> (!user.getNick().equalsIgnoreCase("Timmy") 
											 && (size <= 2 || !user.getNick().equalsIgnoreCase("Skynet"))
											 && !Tim.db.ignore_list.contains(user.getNick().toLowerCase())
											 && !Tim.db.soft_ignore_list.contains(user.getNick().toLowerCase())
				)).forEach(finalUsers::add);

			if (finalUsers.size() > 0) {
				int r = Tim.rand.nextInt(finalUsers.size());
				int i = 0;

				for (User user : finalUsers) {
					if (i == r) {
						sender = user;
					}
					i++;
				}
			} else {
				actions = new String[]{
					"eightball", "sing", "dance", "summon"
				};
			}
		}

		Set<String> enabled_actions = new HashSet<>(16);
		for (String action : actions) {
			if (cdata.chatter_enabled.get(action)) {
				enabled_actions.add(action);
			}
		}

		if (enabled_actions.isEmpty()) {
			return;
		}

		String action = enabled_actions.toArray(new String[enabled_actions.size()])[Tim.rand.nextInt(enabled_actions.size())];
		Channel sendChannel = Tim.channelStorage.channelList.get(channel);

		switch (action) {
			case "item":
				assert sender != null;
				getItem(sendChannel, sender.getNick(), null);
				break;
			case "eightball":
				eightball(sendChannel, sender, true, "");
				break;
			case "fridge":
				Fridge.throwFridge(sendChannel, sender, null, false);
				break;
			case "defenestrate":
				defenestrate(sendChannel, sender, null, false);
				break;
			case "sing":
				sing(sendChannel);
				break;
			case "foof":
				foof(sendChannel, sender, null, false);
				break;
			case "dance":
				dance(sendChannel);
				break;
			case "summon":
				summon(sendChannel, null, false);
				break;
			case "banish":
				banish(sendChannel, null, false);
				break;
			case "catch":
				catchCommand(sendChannel, null);
				break;
			case "search":
				search(sendChannel, sender, null);
				break;
			case "herd":
				herd(sendChannel, sender, null);
				break;
		}
	}

	private void dice(String number, MessageEvent event) {
		int max;
		try {
			max = Integer.parseInt(number);
			int r = Tim.rand.nextInt(max) + 1;
			if (r < 9000) {
				event.respond("Your result is " + r);
			} else {
				event.respond("OVER 9000!!! (" + r + ")");
			}
		} catch (NumberFormatException ex) {
			event.respond(number + " is not a number I could understand.");
		}
	}

	private void attackCommand(Channel channel, User sender, String target) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		String item = this.approvedItems.get(Tim.rand.nextInt(this.approvedItems.size()));

		if (target != null && Tim.rand.nextInt(100) > 75) {
			channel.send().action(String.format("decides he likes %s, so he attacks %s instead...", target, sender.getNick()));
			target = sender.getNick();
		} else if (target == null) {
			target = sender.getNick();
		}

		int damage;

		switch(Tim.rand.nextInt(8)) {
			case 2:
			case 3:
				damage = Tim.rand.nextInt(100);
				break;
			case 4:
			case 5:
				damage = Tim.rand.nextInt(1000);
				break;
			case 6:
				damage = Tim.rand.nextInt(10000);
				break;
			case 7:
				damage = Tim.rand.nextInt(100000);
				break;
			default:
				damage = Tim.rand.nextInt(10);
		}

		String damageString;
		if (damage > 9000) {
			damageString = "OVER 9000";
		} else {
			damageString = formatter.format(damage);
		}
		
		channel.send().action(String.format("hits %s with %s for %s points of damage.", target, item, damageString));
	}

	private void getItem(Channel channel, String target, String[] args) {
		StringBuilder item = new StringBuilder();
		if (args != null) {
			if (Tim.rand.nextInt(100) < 65) {
				item = new StringBuilder(args[0]);
				for (int i = 1; i < args.length; ++i) {
					item.append(" ").append(args[i]);
				}

				if (!(this.approvedItems.contains(item.toString()) || this.pendingItems.contains(item.toString())) && item.length() < 300) {
					this.insertPendingItem(item.toString());
					this.pendingItems.add(item.toString());
				}

				if (item.toString().toLowerCase().contains("spoon")) {
					item = new StringBuilder();
					channel.send().action("rummages around in the back room for a bit, then calls out. \"Sorry... there is no spoon. Maybe this will do...\"");
				}
			} else {
				channel.send().action("rummages around in the back room for a bit, then calls out. \"Sorry... I don't think I have that. Maybe this will do...\"");
			}
		}

		if (item.length() == 0) {
			// Find a random item.
			int i = Tim.rand.nextInt(this.approvedItems.size());
			item = new StringBuilder(this.approvedItems.get(i));
		}

		channel.send().action(String.format("gets %s %s.", target, item.toString()));
	}

	private void getItemFrom(Channel channel, String recipient, String target, String[] args) {
		StringBuilder item = new StringBuilder();
		if (args != null) {
			if (Tim.rand.nextInt(100) < 65) {
				item = new StringBuilder(args[0]);
				for (int i = 1; i < args.length; ++i) {
					item.append(" ").append(args[i]);
				}

				if (!(this.approvedItems.contains(item.toString()) || this.pendingItems.contains(item.toString())) && item.length() < 300) {
					this.insertPendingItem(item.toString());
					this.pendingItems.add(item.toString());
				}

				if (item.toString().toLowerCase().contains("spoon")) {
					item = new StringBuilder();
					channel.send().action("rummages around in " + target + "'s things for a bit, then calls out. \"Sorry... there is no spoon. But I did find something else...\"");
				}
			} else {
				channel.send().action("rummages around in " + target + "'s things for a bit, then calls out. \"Sorry... I don't think they have that. But I did find something else...\"");
			}
		}

		if (item.length() == 0) {
			// Find a random item.
			int i = Tim.rand.nextInt(this.approvedItems.size());
			item = new StringBuilder(this.approvedItems.get(i));
		}

		channel.send().action(String.format("takes %s from %s, and gives it to %s.", item.toString(), target, recipient));
	}

	private void search(Channel channel, User sender, String target) {
		StringBuilder item = new StringBuilder();

		int count = Tim.rand.nextInt(4);
		if (count == 1) {
			item = new StringBuilder(this.approvedItems.get(Tim.rand.nextInt(this.approvedItems.size())));
		} else if (count > 1) {
			for (int i = 0; i < count; i++) {
				if (i > 0 && count > 2) {
					item.append(",");
				}

				if (i == (count - 1)) {
					item.append(" and ");
				} else if (i > 0) {
					item.append(" ");
				}

				item.append(this.approvedItems.get(Tim.rand.nextInt(this.approvedItems.size())));
			}
		}

		if (target != null && Tim.rand.nextInt(100) > 75) {
			channel.send().action("decides at the last second to search " + sender.getNick() + "'s things instead...");
			target = sender.getNick();
		} else {
			if (target == null) {
				target = sender.getNick();
			}

			channel.send().action("searches through " + target + "'s things, looking for contraband...");
		}

		if (item.toString().equals("")) {
			channel.send().action(String.format("can't find anything, and grudgingly clears %s.", target));
		} else {
			channel.send().action(String.format("reports %s to Skynet for possesion of %s.", target, item.toString()));
		}
	}

	private void lick(MessageEvent event, String[] args) {
		if (args != null && args.length >= 1) {
			String argStr = StringUtils.join(args, " ");

			event.getChannel().send().action("licks " + argStr + ". Tastes like " + this.flavours.get(Tim.rand.nextInt(this.flavours.size())));
		} else if (event.getUser() != null) {
				event.getChannel().send().action("licks " + event.getUser().getNick() + "! Tastes like " + this.flavours.get(Tim.rand.nextInt(this.flavours.size())));
		}
	}
	
	void pickone(MessageEvent event, String[] args) {
		String argStr = StringUtils.join(args, " ");
		String[] choices = argStr.split(",", 0);

		if (choices.length < 1) {
			event.respond("I don't see any choices there!");
		} else if (choices.length == 1) {
			event.respond("You only gave one option, silly!");
		} else {
			int r = Tim.rand.nextInt(choices.length);
			event.respond("I pick... " + choices[r]);
		}
	}

	void eightball(Channel channel, User sender, boolean mutter, String argStr) {
		try {
			int r = Tim.rand.nextInt(Tim.db.eightBalls.size());
			int delay = Tim.rand.nextInt(1000) + 1000;
			Thread.sleep(delay);

			if (mutter) {
				channel.send().action("mutters under his breath, \"" + Tim.db.eightBalls.get(r) + "\"");
			} else {
				if (Tim.rand.nextInt(100) < 5) {
					channel.send().message(sender.getNick() + ": " + Tim.markov.generate_markov("say", argStr));
				} else {
					channel.send().message(sender.getNick() + ": " + Tim.db.eightBalls.get(r));
				}
			}
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void sing(Channel channel) {
		Connection con;
		int r = Tim.rand.nextInt(100);

		String response;
		if (r > 90) {
			response = "sings the well known song '%s' better than the original artist!";
		} else if (r > 60) {
			response = "chants some obscure lyrics from '%s'. At least you think that's the name of the song...";
		} else if (r > 30) {
			response = "starts singing '%s'. You've heard better...";
		} else {
			response = "screeches out some words from '%s', and all the nearby windows shatter... Ouch.";
		}

		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement songNameQuery = con.prepareStatement("SELECT name FROM songs ORDER BY rand() LIMIT 1");
			ResultSet songNameRes;

			songNameRes = songNameQuery.executeQuery();
			songNameRes.next();

			String songName = songNameRes.getString("name");
			con.close();

			r = Tim.rand.nextInt(500) + 500;
			Thread.sleep(r);
			channel.send().action(String.format(response, songName));
		} catch (SQLException | InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void dance(Channel channel) {
		Connection con;
		int r = Tim.rand.nextInt(100);

		String response;
		if (r > 90) {
			response = "dances the %s so well he should be on Dancing with the Stars!";
		} else if (r > 60) {
			response = "does the %s, and tears up the dance floor.";
		} else if (r > 30) {
			response = "attempts to do the %s, but obviously needs more practice.";
		} else {
			response = "flails about in a fashion that vaguely resembles the %s. Sort of.";
		}

		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement danceName = con.prepareStatement("SELECT name FROM dances ORDER BY rand() LIMIT 1");
			ResultSet danceNameRes;

			danceNameRes = danceName.executeQuery();
			danceNameRes.next();

			int delay = Tim.rand.nextInt(500) + 500;
			Thread.sleep(delay);
			channel.send().action(String.format(response, danceNameRes.getString("name")));
			con.close();
		} catch (SQLException | InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void boxodoom(MessageEvent event, String[] args) {
		int goal;
		long duration, base_wpm;
		double modifier;

		String difficulty = "", original_difficulty = "";
		Connection con;

		if (args.length != 2) {
			event.respond("!boxodoom requires two parameters.");
			return;
		}

		try {
			if (Pattern.matches("(?i)((extra|super)?easy)|average|medium|normal|hard|extreme|insane|impossible|tadiera", args[0])) {
				original_difficulty = difficulty = args[0].toLowerCase();
				duration = (long) Double.parseDouble(args[1]);
			} else if (Pattern.matches("(?i)((extra|super)?easy)|average|medium|normal|hard|extreme|insane|impossible|tadiera", args[1])) {
				original_difficulty = difficulty = args[1].toLowerCase();
				duration = (long) Double.parseDouble(args[0]);
			} else {
				event.respond("Difficulty must be one of: extraeasy, easy, average, hard, extreme, insane, impossible, tadiera");
				return;
			}
		} catch (NumberFormatException ex) {
			duration = 0;
		}

		if (duration < 1) {
			event.respond("Duration must be greater than or equal to 1.");
			return;
		}

		switch (difficulty) {
			case "extraeasy":
			case "supereasy":
				difficulty = "easy";
				break;
			case "medium":
			case "normal":
				difficulty = "average";
				break;
			case "extreme":
			case "insane":
			case "impossible":
			case "tadiera":
				difficulty = "hard";
				break;
		}

		String value = "";
		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `box_of_doom` WHERE `difficulty` = ? ORDER BY rand() LIMIT 1");
			s.setString(1, difficulty);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("challenge");
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		base_wpm = (long) Double.parseDouble(value);
		switch (original_difficulty) {
			case "extraeasy":
			case "supereasy":
				base_wpm *= 0.65;
				break;
			case "extreme":
				base_wpm *= 1.4;
				break;
			case "insane":
				base_wpm *= 1.8;
				break;
			case "impossible":
				base_wpm *= 2.2;
				break;
			case "tadiera":
				base_wpm *= 3;
				break;
		}

		modifier = 1.0 / Math.log(duration + 1.0) / 1.5 + 0.68;
		goal = (int) (duration * base_wpm * modifier / 10) * 10;

		event.respond("Your goal is " + String.valueOf(goal));
	}

	private void commandment(Channel channel, String[] args) {
		int r = Tim.rand.nextInt(this.commandments.size());
		if (args != null && args.length == 1 && Double.parseDouble(args[0]) > 0
			&& Double.parseDouble(args[0]) <= this.commandments.size()) {
			r = (int) Double.parseDouble(args[0]) - 1;
		}

		channel.send().message(this.commandments.get(r));
	}

	private void herd(Channel channel, User sender, String[] args) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");

				for (User t : Tim.db.channel_data.get(channel.getName().toLowerCase()).userList.values()) {
					if (t.getNick().toLowerCase().equals(target.toLowerCase())) {
						target = t.getNick();
						break;
					}
				}
			}

			int time;
			time = Tim.rand.nextInt(1000) + 500;
			Thread.sleep(time);
			channel.send().action("collects several " + Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size())) + " boxes, and lays them around to attract cats...");

			int i = Tim.rand.nextInt(100);
			String herd = Tim.db.cat_herds.get(Tim.rand.nextInt(Tim.db.cat_herds.size()));
			String act;

			if (i > 33) {				
				act = String.format(herd, target, target);
			} else if (i > 11) {
				target = sender.getNick();
				act = String.format("gets confused and " + herd, target, target);
			} else {
				act = "can't seem to find any cats. Maybe he used the wrong color of box?";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void defenestrate(Channel channel, User sender, String[] args, Boolean righto) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");

				for (User t : Tim.db.channel_data.get(channel.getName().toLowerCase()).userList.values()) {
					if (t.getNick().toLowerCase().equals(target.toLowerCase())) {
						target = t.getNick();
						break;
					}
				}
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time;
			time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send().action("looks around for a convenient window, then slinks off...");

			int i = Tim.rand.nextInt(100);

			String act;
			String colour = Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size()));
			if (i > 33) {
				act = "throws " + target + " through the nearest window, where they land on a giant pile of fluffy " + colour + " pillows.";
			} else if (i > 11) {
				target = sender.getNick();
				act = "laughs maniacally then throws " + target + " through the nearest window, where they land on a giant pile of fluffy " + colour + " pillows.";
			} else {
				act = "trips and falls out the window!";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void summon(Channel channel, String[] args, Boolean righto) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = this.deities.get(Tim.rand.nextInt(this.deities.size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time;
			time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send().action("prepares the summoning circle required to bring " + target + " into the world...");

			int i = Tim.rand.nextInt(100);
			String act;

			if (i > 50) {
				act = "completes the ritual successfully, drawing " + target + " through, and binding them into the summoning circle!";
			} else if (i > 30) {
				act = "completes the ritual, drawing " + target + " through, but something goes wrong and they fade away after just a few moments.";
			} else {
				String target2 = this.deities.get(Tim.rand.nextInt(this.deities.size()));
				act = "attempts to summon " + target + ", but something goes horribly wrong. After the smoke clears, " + target2 + " is left standing on the smoldering remains of the summoning circle.";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void banish(Channel channel, String[] args, Boolean righto) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = this.deities.get(Tim.rand.nextInt(this.deities.size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time;
			time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send().action("gathers the supplies necessary to banish " + target + " to the outer darkness...");

			int i = Tim.rand.nextInt(100);
			String act;

			if (i > 50) {
				act = "completes the ritual successfully, banishing " + target + " to the outer darkness, where they can't interfere with Timmy's affairs!";
			} else if (i > 30) {
				act = "completes the ritual to banish " + target + ", but they reappear after a short absence, looking a bit annoyed.";
			} else {
				String target2 = this.deities.get(Tim.rand.nextInt(this.deities.size()));
				act = "attempts to banish " + target + ", but something goes horribly wrong. As the ritual is completed, " + target2 + " appears to chastise "+Tim.bot.getNick()+" for his temerity.";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void catchCommand(Channel channel, String[] args) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = Tim.db.pokemon.get(Tim.rand.nextInt(Tim.db.pokemon.size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			String colour1 = Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size()));
			String colour2;
			do {
				colour2 = Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size()));
			} while(Objects.equals(colour2, colour1));

			channel.send().action(String.format("grabs a %s and %s pokeball, and attempts to catch %s!", colour1, colour2, target));

			int i = Tim.rand.nextInt(100);
			String act;

			if (i > 65) {
				act = String.format("catches %s, and adds them to his collection!", target);
			} else if (i > 50) {
				act = String.format("almost catches %s, but they manage to break out of the pokeball and escape!", target);
			} else if (i > 25) {
				String target2;
				do {
					target2 = Tim.db.pokemon.get(Tim.rand.nextInt(Tim.db.pokemon.size()));
				} while (Objects.equals(target2, target));

				act = String.format("somehow misses %s, and his pokeball captures %s instead. Oops!", target, target2);
			} else {
				act = "misses his throw completely, and catches nothing. Awwwww.";
			}

			Thread.sleep(1000);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void foof(Channel channel, User sender, String[] args, Boolean righto) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send().action("surreptitiously works his way over to the couch, looking ever so casual...");
			int i = Tim.rand.nextInt(100);
			String act;
			String colour = Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size()));

			if (i > 33) {
				act = "grabs a " + colour + " pillow, and throws it at " + target
					+ ", hitting them squarely in the back of the head.";
			} else if (i > 11) {
				target = sender.getNick();
				act = "laughs maniacally then throws a " + colour + " pillow at "
					+ target
					+ ", then runs off and hides behind the nearest couch.";
			} else {
				act = "trips and lands on a " + colour + " pillow. Oof!";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Logger.getLogger(Amusement.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getApprovedItems() {
		Connection con;
		String value;

		try {
			this.approvedItems.clear();

			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = TRUE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.approvedItems.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getPendingItems() {
		Connection con;
		String value;
		this.pendingItems.clear();

		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = FALSE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.pendingItems.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void insertPendingItem(String item) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `items` (`item`, `approved`) VALUES (?, FALSE)");
			s.setString(1, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setItemApproved(String item, Boolean approved) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `items` SET `approved` = ? WHERE `item` = ?");
			s.setBoolean(1, approved);
			s.setString(2, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void removeItem(String item) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `items` WHERE `item` = ?");
			s.setString(1, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getAypwipList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `aypwips`");

			ResultSet rs = s.getResultSet();
			this.ponderingList.clear();
			while (rs.next()) {
				this.ponderingList.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getCommandmentList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `commandments`");

			ResultSet rs = s.getResultSet();
			this.commandments.clear();
			while (rs.next()) {
				this.commandments.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getDeityList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `deities`");

			ResultSet rs = s.getResultSet();
			this.deities.clear();
			while (rs.next()) {
				this.deities.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getFlavourList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `flavours`");

			ResultSet rs = s.getResultSet();
			this.flavours.clear();
			while (rs.next()) {
				this.flavours.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
