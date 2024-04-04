module XOR_32bit (
    input [31:0] A,
    input [31:0] B,
    output [31:0] Y
);

    assign Y = A ^ B;

endmodule

module OR_32bit (
    input [31:0] A,
    input [31:0] B,
    output [31:0] Y
);

    assign Y = A | B;

endmodule

module AND_32bit (
    input [31:0] A,
    input [31:0] B,
    output [31:0] Y
);

    assign Y = A & B;

endmodule

module signal_Extend (
    input A,
    output [31:0] Y
);

    assign Y = {32{A}};

endmodule

module zero_Extend (
    input A,
    output [31:0] Y
);

    assign Y = {{31{1'b0}}, A};

endmodule

module Barrel_Shifter (
    input [31:0] Din,
    input [4:0] Shamt,
    input L_R,
    input A_L,
    output [31:0] Y
);

    assign Y = A_L ? (L_R ? (Din << Shamt) : (Din >> Shamt)) : (L_R ? (Din <<< Shamt) : (Din >>> Shamt));

endmodule

module ALU_Control (
    input [3:0] ALUctr,
    output A_L,
    output L_R,
    output U_S,
    output Sub_Add
);

    MuxKeyWithDefault #(1, 4, 1) mux_SA (Sub_Add, ALUctr, 1'b1, {
        4'b0000, 1'b0
    });

    MuxKeyWithDefault #(1, 4, 1) mux_AL (A_L, ALUctr, 1'b1, {
        4'b1010, 1'b0
    });

    MuxKeyWithDefault #(1, 3, 1) mux_LR (L_R, ALUctr[2:0], 1'b0, {
        3'b001, 1'b1
    });

    MuxKeyWithDefault #(1, 4, 1) mux_US (U_S, ALUctr, 1'b0, {
        4'b1010, 1'b1
    });

    // always @(*) begin
    //     case(ALUctr)
    //         2'b0000: Sub_Add = 0;
    //         default: Sub_Add = 1;
    //     endcase
    // end

    // always @(*) begin
    //     case(ALUctr)
    //         2'b0101: A_L = 0;
    //         default: A_L = 1;
    //     endcase
    // end

    // always @(*) begin
    //     case(ALUctr[2:0])
    //         2'b001: L_R = 1;
    //         default: L_R = 0;
    //     endcase
    // end

    // always @(*) begin
    //     case(ALUctr)
    //         2'b1010: U_S = 1;
    //         default: U_S = 0;
    //     endcase
    // end

endmodule

module Adder (
    input [31:0] A,
    input [31:0] B,
    input Cin,

    output [31:0] Result,
    output Carry,
    output Zero,
    output Overflow
);
    wire [31:0] R_B;
    assign R_B = B + {{31{1'b0}},Cin};

    assign {Carry, Result} = A + R_B;
    assign Zero = (Result == 0);
    assign Overflow = (A[31] & R_B[31] & ~Result[31]) | (~A[31] & ~R_B[31] & Result[31]);

endmodule

module ALU (
    input [3:0] ALUctr,
    input [31:0] A,
    input [31:0] B,

    output [31:0] ALUout,
    output Zero,
    output Less
);
    //The output signal form the ALU control module
    wire A_L;
    wire L_R;
    wire U_S;
    wire Sub_Add;
    //The output signal form the pre extend before adder
    wire [31:0] extend_Sub_add;
    wire [31:0] B1;
    //The output signal form the adder
    wire [31:0] Result;
    wire Carry;
    wire Overflow;
    //The input signal need by mux81
    wire [31:0] shift;
    wire [31:0] slt;
    wire [31:0] AND;
    wire [31:0] OR;
    wire [31:0] XOR;

    ALU_Control my_ALU_Control (
        .ALUctr(ALUctr),
        .A_L(A_L),
        .L_R(L_R),
        .U_S(U_S),
        .Sub_Add(Sub_Add)
    );


    signal_Extend sub_Add_signal_extend (
        .A(Sub_Add),
        .Y(extend_Sub_add)
    );

    XOR_32bit pre_XOR_for_adder (
        .A(B),
        .B(extend_Sub_add),
        .Y(B1)
    );

    Adder my_adder (
        .A(A),
        .B(B1),
        .Cin(Sub_Add),
        .Result(Result),
        .Carry(Carry),
        .Zero(Zero),
        .Overflow(Overflow)
    );

    mux21 mux21_get_less (
        .a(Result[31] ^ Overflow),
        .b(Sub_Add ^ Carry),
        .s(U_S),
        .y(Less)
    );

    Barrel_Shifter ALUoutput_shift (
        .Din(A),
        .Shamt(B[4:0]),
        .L_R(L_R),
        .A_L(A_L),
        .Y(shift)
    );

    zero_Extend ALUoutput_slt (
        .A(Less),
        .Y(slt)
    );

    XOR_32bit ALUoutput_xor (
        .A(A),
        .B(B),
        .Y(XOR)
    );

    OR_32bit ALUoutput_or (
        .A(A),
        .B(B),
        .Y(OR)
    );

    AND_32bit ALUoutput_and (
        .A(A),
        .B(B),
        .Y(AND)
    );

    MuxKeyWithDefault #(8, 3, 32) mux81 (ALUout, ALUctr[2:0], 32'b0, {
        3'b000, Result,
        3'b001, shift,
        3'b010, slt,
        3'b011, B,
        3'b100, XOR,
        3'b101, shift,
        3'b110, OR,
        3'b111, AND
    });

    // MuxKeyWithDefault#(
    //     .NR_KEY(8),
    //     .KEY_LEN(3),
    //     .DATA_LEN(32)
    // ) mux81 (
    //     .out(ALUout),
    //     .key(ALUctr[2:0]),
    //     .default_out(32'b0),
    //     .lut({
    //         Result,
    //         shift,
    //         slt,
    //         B,
    //         XOR,
    //         shift,
    //         OR,
    //         AND,
    //     })
    // );

endmodule

