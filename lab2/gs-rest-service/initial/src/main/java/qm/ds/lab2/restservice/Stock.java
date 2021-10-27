package qm.ds.lab2.restservice;

import java.util.HashMap;
import java.util.Map;

public class Stock {

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
		
		return null; // return err view here
	}
	
	public Stock changeStock(String item, int n) {
		
		if (this.entryIsValid(item, n) && this.getStock().containsKey(item)) {
			
			this.getStock().put(item, n);
			return this;
		}
		
		return null; // return err view here
		
	}
	
	public Stock setStock (String item, int stockLevel) { return this.changeStock(item, stockLevel); }
	public Stock addStock    (String item, int numItem) { return this.changeStock(item, getStock().get(item) +numItem); }
	public Stock removeStock (String item, int numItem) { return this.changeStock(item, getStock().get(item) -numItem); }
}
