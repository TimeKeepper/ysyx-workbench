# PROJECT DIRECTORIES AND FILES DEFIENITIONS

PRJ = playground

BUILD_DIR = ./build

SRC_DIR = ./src

SSRC_DIR = $(SRC_DIR)/ssrc

SSRC = $(shell find $(abspath $(SSRC_DIR)) -name "*.scala")

$(shell mkdir -p $(BUILD_DIR))

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: clean

V_FILE_GEN = $(BUILD_DIR)/II_4.sv
V_FILE_OVERWRITE = $(BUILD_DIR)/II_4_ov.sv

verilog: $(SSRC)
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(BUILD_DIR)
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(V_FILE_GEN)
	sed -i '/firrtl_black_box_resource_files.f/, $$d' $(V_FILE_GEN)
	@verilator -E -P $(V_FILE_GEN) >| $(V_FILE_OVERWRITE)
