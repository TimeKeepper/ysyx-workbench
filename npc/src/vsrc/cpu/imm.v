module imm_gen(
    input [31:0] inst,
    input [2:0]  extop,

    output [31:0] imm
);

MuxKeyWithDefault #(5, 3, 32) imm_get (imm, extop, 32'h00000000, {
    3'b000, {{20{inst[31]}}, inst[31:20]},
    3'b001, {inst[31:12], 12'b0},
    3'b010, {{20{inst[31]}}, inst[31:25], inst[11:7]},
    3'b011, {{20{inst[31]}}, inst[7], inst[30:25], inst[11:8], 1'b0},
    3'b100, {{12{inst[31]}}, inst[19:12], inst[20], inst[30:21], 1'b0}
});

endmodule