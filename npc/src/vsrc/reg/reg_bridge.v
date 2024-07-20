import "DPI-C" function void cpu_value_update(input bit pc_wen, input bit csra_wen, input bit csrb_wen, input bit gpr_wen, input int unsigned new_PC, input int unsigned CSR_waddra, input int unsigned new_CSRa, input int unsigned CSR_waddrb, input int unsigned new_CSRb, input int unsigned GPR_waddr, input int unsigned new_GPR);

module reg_bridge(
    input clock,
    input pc_wen,
    input csra_wen,
    input csrb_wen,
    input gpr_wen,
    input [31:0] new_pc,
    input [11:0] CSR_waddra,
    input [11:0] CSR_waddrb,
    input [31:0] new_CSRa,
    input [31:0] new_CSRb,
    input [4:0] GPR_waddr,
    input [31:0] new_GPR
);

    always_ff @(posedge clock) begin
        cpu_value_update(pc_wen, csra_wen, csrb_wen, gpr_wen, new_pc, {20'h00000, CSR_waddra}, new_CSRa, {20'h00000, CSR_waddrb}, new_CSRb, {27'h0000000, GPR_waddr}, new_GPR);
    end

endmodule
