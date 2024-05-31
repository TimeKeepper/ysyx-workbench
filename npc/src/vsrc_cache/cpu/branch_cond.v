module branch_cond(
    input [2:0] Branch,
    input Less,
    input Zero,

    output PCAsrc,
    output PCBsrc
);

MuxKeyWithDefault #(6, 3, 1) Abranch_mux(PCAsrc, Branch, 1'b0, {
    3'b001, 1'b1,
    3'b010, 1'b1,
    3'b100, Zero,
    3'b101, ~Zero,
    3'b110, Less,
    3'b111, ~Less
});

MuxKeyWithDefault #(1, 3, 1) Bbranch_mux(PCBsrc, Branch, 1'b0, {
    3'b010, 1'b1
});

endmodule