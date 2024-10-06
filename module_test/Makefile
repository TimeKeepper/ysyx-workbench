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

V_FILE_GEN = $(BUILD_DIR)/II_2.sv
V_FILE_OVERWRITE = $(BUILD_DIR)/II_2_ov.sv

verilog: $(SSRC)
	$(call git_commit, "generate verilog")
	# mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(BUILD_DIR)
	@verilator -E -P $(V_FILE_GEN) >| $(V_FILE_OVERWRITE)
