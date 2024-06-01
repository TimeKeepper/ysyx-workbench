import "DPI-C" function int npc_trap (input int ra);

module BlackBoxDPIC(
    input [31:0] inst
);

always @(inst) begin
    if(inst == 32'h00100073)
        $display("sim has been stop at clk_cnt %d", npc_trap(cpu.REG.gpr_10));
end

endmodule