# Clean up HardwareCollector which is being removed.
$(call add-clean-step, rm -rf $(OUT_DIR)/target/common/obj/APPS/HardwareCollector_intermediates)
$(call add-clean-step, rm -rf $(PRODUCT_OUT)/system/priv-app/HardwareCollector)
