verilog:
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(VSRC_DIR)
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(V_FILE_GEN)
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $(V_FILE_GEN)
