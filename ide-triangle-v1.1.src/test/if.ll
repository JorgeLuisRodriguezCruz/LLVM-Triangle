@last_char = global i32 0

; Format strings
@.str.int = private unnamed_addr constant [4 x i8] c"%d\0A\00", align 1
@.str.int.scanf = private unnamed_addr constant [3 x i8] c"%d\00", align 1
@.str.char = private unnamed_addr constant [3 x i8] c"%c\00", align 1
@.str.bool = private unnamed_addr constant [4 x i8] c"%s\0A\00", align 1
@.str.true = private unnamed_addr constant [5 x i8] c"true\00", align 1
@.str.false = private unnamed_addr constant [6 x i8] c"false\00", align 1

; Standard Library Functions
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
declare i32 @putchar(i32)
declare i32 @getchar()

define i1 @eol() {
entry:
  %last = load i32, i32* @last_char
  %cmp = icmp eq i32 %last, 10  ; '\n'
  br i1 %cmp, label %true, label %check_eof
  check_eof:
  %cmp2 = icmp eq i32 %last, -1  ; EOF
  br i1 %cmp2, label %true, label %false
  true:
  ret i1 true
  false:
  ret i1 false
}

define void @geteol() {
entry:
  %ch = call i32 @getchar()
  store i32 %ch, i32* @last_char
  ret void
}

define void @puteol() {
entry:
  %result = call i32 @putchar(i32 10)  ; '\n'
  ret void
}

define void @getint(i32* %ptr) {
entry:
  %result = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str.int.scanf, i32 0, i32 0), i32* %ptr)
  ret void
}

define i32 @main() {
entry:
  %i = alloca i32
  %suma = alloca i32
  store i32 1, i32* %i
  store i32 0, i32* %suma
  br label %label0
  label0:
  %temp0 = load i32, i32* %i
  %temp1 = icmp sle i32 %temp0, 10
  br i1 %temp1, label %label1, label %label2
  label1:
  %temp2 = load i32, i32* %i
  %temp3 = icmp slt i32 %temp2, 2
  br i1 %temp3, label %label3, label %label4
  label3:
  %temp4 = load i32, i32* %suma
  %temp5 = load i32, i32* %i
  %temp6 = add i32 %temp4, %temp5
  store i32 %temp6, i32* %suma
  br label %label5
  label4:
  %temp7 = load i32, i32* %suma
  %temp8 = load i32, i32* %i
  %temp9 = add i32 %temp7, %temp8
  %temp10 = mul i32 %temp9, 2
  store i32 %temp10, i32* %suma
  br label %label5
  label5:
  %temp11 = load i32, i32* %i
  %temp12 = add i32 %temp11, 1
  store i32 %temp12, i32* %i
  br label %label0
  label2:
  call i32 @putchar(i32 83)
  %temp13 = load i32, i32* %suma
  call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str.int, i32 0, i32 0), i32 %temp13)
  ret i32 0
}
