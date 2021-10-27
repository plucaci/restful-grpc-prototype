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
	public Stock addItem(@RequestParam(value = "item", defaultValue = "") String item) {
		return this.getStockInstance().addItem(item);
	}
	
	@GetMapping("/stock/setStock")
	public Stock setStock(@RequestParam(value = "item", defaultValue = "") String item, @RequestParam(value = "stockLevel", defaultValue = "") int stockLevel) {
		return this.getStockInstance().setStock(item, stockLevel);
	}
	
	@GetMapping("/stock/addStock")
	public Stock addStock(@RequestParam(value = "item", defaultValue = "") String item, @RequestParam(value = "numItem", defaultValue = "") int numItem) {
		return this.getStockInstance().addStock(item, numItem);
	}
	
	@GetMapping("/stock/removeStock")
	public Stock removeStock(@RequestParam(value = "item", defaultValue = "") String item, @RequestParam(value = "numItem", defaultValue = "") int numItem) {
		return this.getStockInstance().removeStock(item, numItem);
	}
	
}