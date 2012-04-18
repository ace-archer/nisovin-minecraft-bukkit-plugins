package com.nisovin.bookworm.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.bookworm.Book;

public class BookCopyEvent extends BookEvent {

    private static final HandlerList handlers = new HandlerList();

	private Player player;
	private Location location;
	
	public BookCopyEvent(Player player, Book book, Location location) {
		super(book);
	}

	public Player getPlayer() {
		return player;
	}
	
	public Location getLocation() {
		return location;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }	

}
