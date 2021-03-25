package qm.ds.lab2.restservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class StockController {
	
	private final AtomicLong counter = new AtomicLong();
	
	public Stock stockInstance = null;
	
	@GetMapping("/stock")
	public Stock getStockInstance() {
		
		if (stockInstance == null) {
			stockInstance = new Stock(counter.getAndIncrement());
		}
		
		return stockInstance;
	}
	
	@GetMapping("/stock/list")
	public Map<String, Integer> listStock() {
		return this.getStockInstance().getStock();
	}
	
	@GetMapping("/stock/addItem")
	public Response addItem(@RequestParam(value = "item", defaultValue = "") String item) {
		
		Stock addItem = this.getStockInstance().addItem(item.toLowerCase());
		
		if (addItem == null) {
			return new ResponseErrors("Invalid Operation");
		}
		return addItem;
	}
	
	@GetMapping("/stock/setStock")
	public Response setStock(@RequestParam(value = "item", defaultValue = "") String item, 
			@RequestParam(value = "stockLevel", defaultValue = "") int stockLevel) {
		
		Stock setStock = this.getStockInstance().setStock(item.toLowerCase(), stockLevel);
		
		if (setStock == null) {
			return new ResponseErrors("Invalid Operation");
		}
		return setStock;
	}
	
	@GetMapping("/stock/addStock")
	public Response addStock(@RequestParam(value = "item", defaultValue = "") String item, 
			@RequestParam(value = "numItem", defaultValue = "") int numItem) {
		
		Stock addStock = this.getStockInstance().addStock(item.toLowerCase(), numItem);
		
		if (addStock == null) {
			return new ResponseErrors("Invalid Operation");
		}
		return addStock;
	}
	
	@GetMapping("/stock/removeStock")
	public Response removeStock(@RequestParam(value = "item", defaultValue = "") String item, 
			@RequestParam(value = "numItem", defaultValue = "") int numItem) {
		
		Stock removeStock = this.getStockInstance().removeStock(item.toLowerCase(), numItem);
		
		if (removeStock == null) {
			return new ResponseErrors("Invalid Operation");
		}
		return removeStock;
	}
}