package qm.ds.lab2.restservice;

import java.util.HashMap;
import java.util.Map;

public class Stock implements Response{

	private final long id;
	private static Map <String, Integer> stock = new HashMap <String, Integer> ();

	public Stock(long id) { this.id = id; }
	
	
	public long getId() {
		return this.id;
	}
	public Map<String, Integer> getStock() {
		return Stock.stock;
	}
	
	
	public boolean entryIsValid(String item, int n) {
		return item != null && !item.equalsIgnoreCase("") && n > -1;
	}
	
	public Stock addItem (String item) {
		
		if (this.entryIsValid(item, 0) && !this.getStock().containsKey(item)) {
			
			this.getStock().put(item, 0);
			return this;
		}
		
		return null; // return an error
	}
	
	public Stock setStock (String item, int stockLevel) {
		
		if (this.entryIsValid(item, stockLevel) && this.getStock().containsKey(item)) {
			
			this.getStock().put(item, stockLevel);
			return this;
		}
		
		return null; // return an error
	}
	
	public Stock addStock (String item, int numItem) {
		
		if (this.entryIsValid(item, numItem) && this.getStock().containsKey(item)) {
			
			int newLevel = getStock().get(item) +numItem;
			if (newLevel < 0) {
				return null; // return an error
			}
			
			this.getStock().put(item, getStock().get(item) +numItem);
			return this;
		}
		
		return null; // return an error
	}
	
	public Stock removeStock (String item, int numItem) {
		
		if (this.entryIsValid(item, numItem) && this.getStock().containsKey(item)) {
			
			int newLevel = getStock().get(item) -numItem;
			if (newLevel < 0) {
				return null;
			}
			
			this.getStock().put(item, getStock().get(item) -numItem);
			return this;
		}
		
		return null; // return an error
	}
}
