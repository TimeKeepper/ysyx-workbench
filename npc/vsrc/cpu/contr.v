module Contr_gen(
    input [6:0] op,
    input [2:0] func3,
    input [6:0] func7,
    input [11:0] priv,

    output [2:0] ExtOp,
    output RegWr,
    output [1:0] ALUAsrc,
    output [1:0] ALUBsrc,
    output [3:0] ALUctr,
    output [2:0] Branch,
    output MemtoReg,
    output MemWr,
    output [2:0] MemOp,
    output [1:0] csr_ctr
);

    MuxKeyWithDefault #(5, 5, 3) ExtOp_mux(ExtOp, op[6:2], 3'b000, {
        5'b01101, 3'b001,
        5'b00101, 3'b001,
        5'b11011, 3'b100,
        5'b11000, 3'b011,
        5'b01000, 3'b010
    });

    wire RegWr_sub11100;
    MuxKeyWithDefault #(3, 3, 1) RegWr_sub11100_mux(RegWr_sub11100, func3, 1'b0, {
        3'b000, 1'b0,
        3'b001, 1'b1,
        3'b010, 1'b1
    });

    MuxKeyWithDefault #(3, 5, 1) RegWr_mux(RegWr, op[6:2], 1'b1, {
        5'b11000, 1'b0,
        5'b01000, 1'b0,
        5'b11100, RegWr_sub11100
    });

    MuxKeyWithDefault #(4, 5, 2) ALUAsrc_mux(ALUAsrc, op[6:2], 2'b00, {
        5'b00101, 2'b01,
        5'b11011, 2'b01,
        5'b11001, 2'b01,
        5'b11100, 2'b10
    });

    MuxKeyWithDefault #(8, 5, 2) ALUBsrc_mux(ALUBsrc, op[6:2], 2'b00, {
        5'b01101, 2'b01,
        5'b00101, 2'b01,
        5'b00100, 2'b01,
        5'b11011, 2'b10,
        5'b11001, 2'b10,
        5'b00000, 2'b01,
        5'b01000, 2'b01,
        5'b11100, 2'b11
    });

    wire [3:0] ALUctr_sub00100;
    wire [3:0] ALUctr_sub01100;
    wire [3:0] ALUctr_sub11000;
    wire [3:0] ALUctr_sub11100;

    MuxKeyWithDefault #(7, 3, 4) ALUctr_sub00100_mux(ALUctr_sub00100, func3, 4'b0000, {
        3'b010, 4'b0010,
        3'b011, 4'b1010,
        3'b100, 4'b0100,
        3'b110, 4'b0110,
        3'b111, 4'b0111,
        3'b001, 4'b0001,
        3'b101, {func7[5], 3'b101}
    });

    MuxKeyWithDefault #(8, 3, 4) ALUctr_sub01100_mux(ALUctr_sub01100, func3, 4'b0000, {
        3'b000, {func7[5], 3'b000},
        3'b001, 4'b0001,
        3'b010, 4'b0010,
        3'b011, 4'b1010,
        3'b100, 4'b0100,
        3'b101, {func7[5], 3'b101},
        3'b110, 4'b0110,
        3'b111, 4'b0111
    });

    MuxKeyWithDefault #(6, 3, 4) ALUctr_sub11000_mux(ALUctr_sub11000, func3, 4'b0000, {
        3'b000, 4'b0010,
        3'b001, 4'b0010,
        3'b100, 4'b0010,
        3'b101, 4'b0010,
        3'b110, 4'b1010,
        3'b111, 4'b1010
    });

    MuxKeyWithDefault #(2, 3, 4) ALUctr_sub11100_mux(ALUctr_sub11100, func3, 4'b0000, {
        3'b001, 4'b0011,
        3'b010, 4'b0110
    });

    MuxKeyWithDefault #(5, 5, 4) ALUctr_mux(ALUctr, op[6:2], 4'b0000, {
        5'b00100, ALUctr_sub00100,
        5'b01100, ALUctr_sub01100,
        5'b11000, ALUctr_sub11000,
        5'b11100, ALUctr_sub11100,
        5'b01101, 4'b0011
    });

    wire [2:0] Sub_Branch;

    MuxKeyWithDefault #(6, 3, 3) Branch_sub_mux(Sub_Branch, func3, 3'b000, {
        3'b000, 3'b100,
        3'b001, 3'b101,
        3'b100, 3'b110,
        3'b101, 3'b111,
        3'b110, 3'b110,
        3'b111, 3'b111
    });

    MuxKeyWithDefault #(3, 5, 3) Branch_mux(Branch, op[6:2], 3'b000, {
        5'b11011, 3'b001,
        5'b11001, 3'b010,
        5'b11000, Sub_Branch
    });

    MuxKeyWithDefault #(1, 5, 1) MemtoReg_mux(MemtoReg, op[6:2], 1'b0, {
        5'b00000, 1'b1
    });

    MuxKeyWithDefault #(1, 5, 1) MemWr_mux(MemWr, op[6:2], 1'b0, {
        5'b01000, 1'b1
    });

    MuxKeyWithDefault #(2, 5, 3) MemOp_mux(MemOp, op[6:2], 3'b000, {
        5'b00000, func3,
        5'b01000, func3
    });

    wire [1:0] csr_ctr_sub000;
    MuxKeyWithDefault #(2, 12, 2) csr_ctr_sub000_mux(csr_ctr_sub000, priv, 2'b00, {
        12'b000000000000, 2'b11,
        12'b001100000010, 2'b01
    });

    MuxKeyWithDefault #(3, 8, 2) csr_ctl_mux(csr_ctr, {op[6:2], func3}, 2'b00, {
        8'b11100001, 2'b10,
        8'b11100010, 2'b10,
        8'b11100000, csr_ctr_sub000
    });

endmodule