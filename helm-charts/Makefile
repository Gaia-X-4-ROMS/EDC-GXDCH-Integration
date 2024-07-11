
connector-start:
	@echo "Starting connector..."
	@helm upgrade -f connector-override-values.yaml --install connector ./connector --namespace gaiax

connector-stop:
	@echo "Stopping connector..."
	@helm uninstall connector --namespace gaiax

dashboard-start:
	@echo "Starting connector dashboard..."
	@helm upgrade -f connector-dashboard-override-values.yaml --install connector-dashboard ./connector-dashboard --namespace gaiax

dashboard-stop:
	@echo "Stopping dashboard..."
	@helm uninstall connector-dashboard --namespace gaiax
