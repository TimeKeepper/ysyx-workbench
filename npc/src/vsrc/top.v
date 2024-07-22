import "DPI-C" function int npc_trap (input int ra);

module top(
    input clk,
    input rst,

    output inst_comp
);

always @(npc.SRAM.bridge.r_data) begin
    if(npc.SRAM.bridge.r_data == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", npc_trap(npc.CPU.REG.gpr_10));
end 


npc npc (
    .clock(clk),
    .reset(rst),

    .io_inst_comp(inst_comp)
);

endmodule